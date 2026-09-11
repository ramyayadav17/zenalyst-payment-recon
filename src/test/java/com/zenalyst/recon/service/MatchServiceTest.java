package com.zenalyst.recon.service;

import com.zenalyst.recon.config.AppSettings;
import com.zenalyst.recon.entity.BankTransaction;
import com.zenalyst.recon.entity.Borrower;
import com.zenalyst.recon.entity.FailedAutoDebit;
import com.zenalyst.recon.entity.LoanAccount;
import com.zenalyst.recon.enums.LoanStatus;
import com.zenalyst.recon.enums.MatchDecision;
import com.zenalyst.recon.repository.FailedAutoDebitRepository;
import com.zenalyst.recon.repository.LoanAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchServiceTest {

    @Mock LoanAccountRepository loanRepo;
    @Mock FailedAutoDebitRepository bounceRepo;

    AppSettings settings;
    MatchService matchService;
    LoanAccount loan;

    @BeforeEach
    void setUp() {
        settings = new AppSettings();
        matchService = new MatchService(loanRepo, bounceRepo, settings);

        Borrower b = new Borrower();
        b.setId(1L);
        b.setFullName("Priya Sharma");
        b.setBorrowerCode("BRW-1");
        b.setKnownPaymentHandles("priya.sharma@oksbi,XXXX1111");

        loan = new LoanAccount();
        loan.setId(10L);
        loan.setLoanAccountNumber("LN10010001");
        loan.setEmiAmount(new BigDecimal("5000.00"));
        loan.setOutstandingPrincipal(new BigDecimal("45000.00"));
        loan.setMandateReference("UMRNPRIYA001");
        loan.setBorrower(b);
        loan.setStatus(LoanStatus.ACTIVE);
    }

    @Test
    void loanNumberAndEmiShouldAutoMatch() {
        when(loanRepo.findActiveWithBorrower()).thenReturn(List.of(loan));
        when(bounceRepo.findOpenInWindow(any(), any())).thenReturn(List.of());

        Optional<ScoredMatch> result = matchService.findBest(txn("NEFT LN10010001 EMI", "5000.00"));

        assertThat(result).isPresent();
        assertThat(result.get().getDecision()).isEqualTo(MatchDecision.AUTO_ACCEPT);
        assertThat(result.get().getScore()).isGreaterThanOrEqualTo(70);
    }

    @Test
    void bounceWindowShouldBoostScore() {
        when(loanRepo.findActiveWithBorrower()).thenReturn(List.of(loan));
        FailedAutoDebit bounce = new FailedAutoDebit();
        bounce.setLoanAccountId(10L);
        bounce.setAttemptDate(LocalDate.now().minusDays(3));
        bounce.setRecovered(false);
        when(bounceRepo.findOpenInWindow(any(), any())).thenReturn(List.of(bounce));

        Optional<ScoredMatch> result = matchService.findBest(txn("NEFT PRIYA SHARMA LOAN EMI", "5000.00"));

        assertThat(result).isPresent();
        assertThat(result.get().isBounceRecovery()).isTrue();
        assertThat(result.get().getDecision()).isEqualTo(MatchDecision.AUTO_ACCEPT);
    }

    @Test
    void twoEmisShouldBeDetected() {
        ScoredMatch m = matchService.score(txn("IMPS ASHA", "10000.00"), loan, null);
        assertThat(m.reasonText()).contains("two-emis");
    }

    private static BankTransaction txn(String narration, String amount) {
        BankTransaction t = new BankTransaction();
        t.setNarration(narration);
        t.setAmount(new BigDecimal(amount));
        t.setTxnDate(LocalDate.now());
        t.setBankReference("T-" + System.nanoTime());
        return t;
    }
}
