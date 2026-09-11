package com.zenalyst.recon.service;

import com.zenalyst.recon.enums.AllocationType;
import com.zenalyst.recon.entity.BankTransaction;
import com.zenalyst.recon.entity.Installment;
import com.zenalyst.recon.enums.InstallmentStatus;
import com.zenalyst.recon.entity.LoanAccount;
import com.zenalyst.recon.enums.LoanStatus;
import com.zenalyst.recon.entity.PaymentAllocation;
import com.zenalyst.recon.repository.FailedAutoDebitRepository;
import com.zenalyst.recon.repository.InstallmentRepository;
import com.zenalyst.recon.repository.LoanAccountRepository;
import com.zenalyst.recon.repository.PaymentAllocationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplyPaymentServiceTest {

    @Mock InstallmentRepository installmentRepo;
    @Mock PaymentAllocationRepository allocationRepo;
    @Mock LoanAccountRepository loanRepo;
    @Mock FailedAutoDebitRepository bounceRepo;

    @InjectMocks ApplyPaymentService applyPaymentService;

    @Test
    void halfPaymentStaysPartial() {
        LoanAccount loan = loan(1L, "32000");
        Installment inst = emi(11L, loan, "4000");
        when(installmentRepo.findOpenByLoan(eq(1L), any())).thenReturn(List.of(inst));
        when(installmentRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(allocationRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(loanRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        BankTransaction txn = txn(99L, "2000");
        List<PaymentAllocation> allocs = applyPaymentService.apply(txn, loan, false);

        assertThat(inst.getStatus()).isEqualTo(InstallmentStatus.PARTIALLY_PAID);
        assertThat(inst.getAmountPaid()).isEqualByComparingTo("2000");
        assertThat(allocs).hasSize(1);
        assertThat(allocs.get(0).getAllocationType()).isEqualTo(AllocationType.INSTALLMENT);
    }

    @Test
    void twoEmisClearBoth() {
        LoanAccount loan = loan(2L, "27000");
        Installment a = emi(21L, loan, "3000");
        Installment b = emi(22L, loan, "3000");
        when(installmentRepo.findOpenByLoan(eq(2L), any())).thenReturn(List.of(a, b));
        when(installmentRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(allocationRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(loanRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        applyPaymentService.apply(txn(100L, "6000"), loan, false);

        assertThat(a.getStatus()).isEqualTo(InstallmentStatus.PAID);
        assertThat(b.getStatus()).isEqualTo(InstallmentStatus.PAID);
    }

    @Test
    void excessCanCloseLoan() {
        LoanAccount loan = loan(3L, "22000");
        Installment inst = emi(31L, loan, "7500");
        when(installmentRepo.findOpenByLoan(eq(3L), any())).thenReturn(List.of(inst));
        when(installmentRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(allocationRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(loanRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        List<PaymentAllocation> allocs = applyPaymentService.apply(txn(101L, "22000"), loan, false);

        assertThat(inst.getStatus()).isEqualTo(InstallmentStatus.PAID);
        assertThat(allocs.stream().anyMatch(a -> a.getAllocationType() == AllocationType.PREPAYMENT)).isTrue();
        assertThat(loan.getStatus()).isEqualTo(LoanStatus.CLOSED);
    }

    private static LoanAccount loan(Long id, String outstanding) {
        LoanAccount loan = new LoanAccount();
        loan.setId(id);
        loan.setOutstandingPrincipal(new BigDecimal(outstanding));
        loan.setStatus(LoanStatus.ACTIVE);
        return loan;
    }

    private static Installment emi(Long id, LoanAccount loan, String due) {
        Installment i = new Installment();
        i.setId(id);
        i.setLoanAccount(loan);
        i.setAmountDue(new BigDecimal(due));
        i.setAmountPaid(BigDecimal.ZERO);
        i.setStatus(InstallmentStatus.DUE);
        i.setDueDate(LocalDate.now());
        return i;
    }

    private static BankTransaction txn(Long id, String amount) {
        BankTransaction t = new BankTransaction();
        t.setId(id);
        t.setAmount(new BigDecimal(amount));
        return t;
    }
}
