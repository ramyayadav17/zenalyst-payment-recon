package com.zenalyst.recon.service;

import com.zenalyst.recon.dto.BankTxnRequest;
import com.zenalyst.recon.entity.BankTransaction;
import com.zenalyst.recon.enums.BankTxnStatus;
import com.zenalyst.recon.entity.LoanAccount;
import com.zenalyst.recon.enums.MatchDecision;
import com.zenalyst.recon.entity.ReconciliationRun;
import com.zenalyst.recon.repository.BankTransactionRepository;
import com.zenalyst.recon.repository.LoanAccountRepository;
import com.zenalyst.recon.repository.ReconciliationRunRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class ReconService {

    private final BankTransactionRepository bankTxnRepo;
    private final LoanAccountRepository loanRepo;
    private final ReconciliationRunRepository runRepo;
    private final MatchService matchService;
    private final ApplyPaymentService applyPaymentService;

    public ReconService(
            BankTransactionRepository bankTxnRepo,
            LoanAccountRepository loanRepo,
            ReconciliationRunRepository runRepo,
            MatchService matchService,
            ApplyPaymentService applyPaymentService) {
        this.bankTxnRepo = bankTxnRepo;
        this.loanRepo = loanRepo;
        this.runRepo = runRepo;
        this.matchService = matchService;
        this.applyPaymentService = applyPaymentService;
    }

    @Transactional
    public BankTransaction addBankTxn(BankTxnRequest req) {
        return bankTxnRepo.findByBankReference(req.getBankReference())
                .orElseGet(() -> {
                    BankTransaction txn = new BankTransaction();
                    txn.setBankReference(req.getBankReference());
                    txn.setTxnDate(req.getTxnDate());
                    txn.setAmount(req.getAmount());
                    txn.setNarration(req.getNarration());
                    txn.setCounterpartyAccount(req.getCounterpartyAccount());
                    txn.setStatus(BankTxnStatus.UNMATCHED);
                    return bankTxnRepo.save(txn);
                });
    }

    @Transactional
    public List<BankTransaction> addBankTxns(List<BankTxnRequest> requests) {
        return requests.stream().map(this::addBankTxn).toList();
    }

    @Transactional(readOnly = true)
    public List<BankTransaction> listByStatus(BankTxnStatus status) {
        return bankTxnRepo.findByStatus(status);
    }

    @Transactional
    public ReconciliationRun runMatching(LocalDate businessDate) {
        ReconciliationRun run = new ReconciliationRun();
        run.setBusinessDate(businessDate);
        run = runRepo.save(run);

        List<BankTransaction> pending = bankTxnRepo.findByTxnDateBetweenAndStatusIn(
                businessDate.minusDays(30),
                businessDate,
                List.of(BankTxnStatus.UNMATCHED, BankTxnStatus.NEEDS_REVIEW));

        int auto = 0;
        int review = 0;
        int unmatched = 0;

        for (BankTransaction txn : pending) {
            Optional<ScoredMatch> best = matchService.findBest(txn);
            if (best.isEmpty() || best.get().getDecision() == MatchDecision.REJECT) {
                txn.setStatus(BankTxnStatus.UNMATCHED);
                txn.setMatchScore(best.map(ScoredMatch::getScore).orElse(0));
                txn.setMatchReasons(best.map(ScoredMatch::reasonText).orElse("no-candidate"));
                bankTxnRepo.save(txn);
                unmatched++;
                continue;
            }

            ScoredMatch hit = best.get();
            txn.setMatchScore(hit.getScore());
            txn.setMatchReasons(hit.reasonText());
            txn.setMatchedLoanId(hit.getLoan().getId());

            if (hit.getDecision() == MatchDecision.AUTO_ACCEPT) {
                applyPaymentService.apply(txn, hit.getLoan(), hit.isBounceRecovery());
                txn.setStatus(BankTxnStatus.AUTO_MATCHED);
                auto++;
            } else {
                txn.setStatus(BankTxnStatus.NEEDS_REVIEW);
                review++;
            }
            bankTxnRepo.save(txn);
        }

        run.setProcessed(pending.size());
        run.setAutoMatched(auto);
        run.setNeedsReview(review);
        run.setUnmatched(unmatched);
        run.setFinishedAt(Instant.now());
        return runRepo.save(run);
    }

    @Transactional
    public BankTransaction confirmMatch(Long txnId, Long loanId) {
        BankTransaction txn = bankTxnRepo.findById(txnId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown bank txn: " + txnId));
        LoanAccount loan = loanRepo.findById(loanId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown loan: " + loanId));

        if (txn.getStatus() == BankTxnStatus.AUTO_MATCHED || txn.getStatus() == BankTxnStatus.MANUALLY_MATCHED) {
            throw new IllegalStateException("Transaction already matched");
        }

        boolean bounce = txn.getMatchReasons() != null
                && txn.getMatchReasons().contains(MatchService.BOUNCE_TAG);
        applyPaymentService.apply(txn, loan, bounce);
        txn.setMatchedLoanId(loanId);
        txn.setStatus(BankTxnStatus.MANUALLY_MATCHED);
        txn.setMatchScore(100);
        txn.setMatchReasons("manual-ops-confirm");
        return bankTxnRepo.save(txn);
    }
}
