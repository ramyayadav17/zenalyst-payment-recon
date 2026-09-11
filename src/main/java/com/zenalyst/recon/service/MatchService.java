package com.zenalyst.recon.service;

import com.zenalyst.recon.config.AppSettings;
import com.zenalyst.recon.entity.BankTransaction;
import com.zenalyst.recon.entity.FailedAutoDebit;
import com.zenalyst.recon.entity.LoanAccount;
import com.zenalyst.recon.enums.MatchDecision;
import com.zenalyst.recon.repository.FailedAutoDebitRepository;
import com.zenalyst.recon.repository.LoanAccountRepository;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class MatchService {

    public static final String BOUNCE_TAG = "bounce-recovery-window";

    private static final Pattern LOAN_NO = Pattern.compile("(?i)(?:LN|LOAN|LA)[-_ ]?(\\d{6,14})");
    private static final Pattern DIGITS = Pattern.compile("\\d{8,14}");

    private final LoanAccountRepository loanRepo;
    private final FailedAutoDebitRepository bounceRepo;
    private final AppSettings settings;
    private final JaroWinklerSimilarity nameSim = new JaroWinklerSimilarity();

    public MatchService(
            LoanAccountRepository loanRepo,
            FailedAutoDebitRepository bounceRepo,
            AppSettings settings) {
        this.loanRepo = loanRepo;
        this.bounceRepo = bounceRepo;
        this.settings = settings;
    }

    public Optional<ScoredMatch> findBest(BankTransaction txn) {
        List<LoanAccount> active = loanRepo.findActiveWithBorrower();
        if (active.isEmpty()) {
            return Optional.empty();
        }

        Map<Long, FailedAutoDebit> bounces = openBouncesNear(txn.getTxnDate()).stream()
                .collect(Collectors.toMap(FailedAutoDebit::getLoanAccountId, f -> f, (a, b) -> a));

        List<ScoredMatch> scored = new ArrayList<>();
        for (LoanAccount loan : active) {
            ScoredMatch m = score(txn, loan, bounces.get(loan.getId()));
            if (m.getScore() > 0) {
                scored.add(m);
            }
        }
        scored.sort(Comparator.comparingInt(ScoredMatch::getScore).reversed());
        if (scored.isEmpty()) {
            return Optional.empty();
        }

        ScoredMatch best = scored.get(0);
        if (scored.size() > 1
                && best.getScore() - scored.get(1).getScore() < 10
                && scored.get(1).getScore() >= settings.getReviewThreshold()) {
            best.setDecision(MatchDecision.REVIEW);
            best.note("ambiguous-top-two");
        } else {
            best.setDecision(decide(best.getScore()));
        }
        return Optional.of(best);
    }

    ScoredMatch score(BankTransaction txn, LoanAccount loan, FailedAutoDebit bounce) {
        ScoredMatch m = new ScoredMatch(loan);
        String narration = Optional.ofNullable(txn.getNarration()).orElse("").toUpperCase(Locale.ROOT);
        String handles = Optional.ofNullable(loan.getBorrower().getKnownPaymentHandles())
                .orElse("").toUpperCase(Locale.ROOT);
        String counterparty = Optional.ofNullable(txn.getCounterpartyAccount()).orElse("").toUpperCase(Locale.ROOT);

        if (hasLoanNumber(narration, loan.getLoanAccountNumber())) {
            m.addPoints(55, "loan-number-in-narration");
        }
        if (loan.getMandateReference() != null
                && !loan.getMandateReference().isBlank()
                && narration.contains(loan.getMandateReference().toUpperCase(Locale.ROOT))) {
            m.addPoints(40, "mandate-reference");
        }
        if (!handles.isBlank()) {
            for (String handle : handles.split("[,;|]")) {
                String h = handle.trim();
                if (h.length() >= 4 && (narration.contains(h) || counterparty.contains(h))) {
                    m.addPoints(35, "known-payment-handle:" + h);
                    break;
                }
            }
        }
        if (!counterparty.isBlank() && handles.contains(counterparty)) {
            m.addPoints(30, "counterparty-account");
        }

        String name = loan.getBorrower().getFullName().toUpperCase(Locale.ROOT);
        double sim = nameScore(narration, name);
        if (sim >= 0.92) {
            m.addPoints(18, "name-strong:" + fmt(sim));
        } else if (sim >= 0.82) {
            m.addPoints(10, "name-weak:" + fmt(sim));
        }

        BigDecimal amount = txn.getAmount();
        BigDecimal emi = loan.getEmiAmount();
        if (amount.compareTo(emi) == 0) {
            m.addPoints(20, "exact-emi");
        } else if (amount.compareTo(emi.multiply(BigDecimal.valueOf(2))) == 0) {
            m.addPoints(18, "two-emis");
        } else if (isHalf(amount, emi)) {
            m.addPoints(12, "half-emi");
        } else if (amount.compareTo(loan.getOutstandingPrincipal()) == 0
                || amount.compareTo(loan.getOutstandingPrincipal().add(emi)) == 0) {
            m.addPoints(22, "foreclosure-like-amount");
        } else if (nearPercent(amount, emi, 2)) {
            m.addPoints(8, "near-emi");
        }

        boolean hasName = m.getReasons().stream().anyMatch(r -> r.startsWith("name-"));
        boolean hasAmt = m.getReasons().stream().anyMatch(r ->
                r.startsWith("exact-emi") || r.startsWith("two-emis") || r.startsWith("half-emi")
                        || r.startsWith("foreclosure") || r.startsWith("near-emi"));
        if (hasName && hasAmt) {
            m.addPoints(15, "name-amount-combo");
        }

        if (bounce != null && withinDays(bounce.getAttemptDate(), txn.getTxnDate(), settings.getBounceRecoveryDays())) {
            long days = ChronoUnit.DAYS.between(bounce.getAttemptDate(), txn.getTxnDate());
            m.addPoints(25, BOUNCE_TAG + ":" + days + "d");
            m.setBounceRecovery(true);
            if (amount.compareTo(emi) == 0 || nearPercent(amount, emi, 5)) {
                m.addPoints(10, "bounce-amount-aligns");
            }
        }
        return m;
    }

    private MatchDecision decide(int score) {
        if (score >= settings.getAutoMatchThreshold()) {
            return MatchDecision.AUTO_ACCEPT;
        }
        if (score >= settings.getReviewThreshold()) {
            return MatchDecision.REVIEW;
        }
        return MatchDecision.REJECT;
    }

    private List<FailedAutoDebit> openBouncesNear(LocalDate txnDate) {
        return bounceRepo.findOpenInWindow(txnDate.minusDays(settings.getBounceRecoveryDays()), txnDate);
    }

    static boolean withinDays(LocalDate from, LocalDate to, int maxDays) {
        if (from == null || to == null) {
            return false;
        }
        long days = ChronoUnit.DAYS.between(from, to);
        return days >= 0 && days <= maxDays;
    }

    private boolean hasLoanNumber(String narration, String loanNo) {
        String normalized = loanNo.toUpperCase(Locale.ROOT);
        if (narration.contains(normalized)) {
            return true;
        }
        String digits = normalized.replaceAll("\\D", "");
        Matcher m = LOAN_NO.matcher(narration);
        while (m.find()) {
            if (normalized.endsWith(m.group(1)) || m.group(1).equals(digits)) {
                return true;
            }
        }
        Matcher d = DIGITS.matcher(narration);
        while (d.find()) {
            if (d.group().equals(digits)) {
                return true;
            }
        }
        return false;
    }

    private double nameScore(String narration, String fullName) {
        double best = nameSim.apply(narration, fullName);
        for (String token : narration.split("[^A-Z]+")) {
            if (token.length() < 3) {
                continue;
            }
            for (String part : fullName.split("\\s+")) {
                if (part.length() < 3) {
                    continue;
                }
                best = Math.max(best, nameSim.apply(token, part));
            }
        }
        return Math.max(best, nameSim.apply(narration.replace(" ", ""), fullName.replace(" ", "")));
    }

    private static boolean isHalf(BigDecimal amount, BigDecimal emi) {
        return amount.compareTo(emi.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP)) == 0;
    }

    private static boolean nearPercent(BigDecimal amount, BigDecimal target, int pct) {
        if (target.compareTo(BigDecimal.ZERO) == 0) {
            return false;
        }
        BigDecimal allowed = target.multiply(BigDecimal.valueOf(pct))
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        return amount.subtract(target).abs().compareTo(allowed) <= 0;
    }

    private static String fmt(double v) {
        return String.format(Locale.ROOT, "%.2f", v);
    }
}
