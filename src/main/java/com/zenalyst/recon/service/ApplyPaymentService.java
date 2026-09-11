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
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

@Component
public class ApplyPaymentService {

    static final List<InstallmentStatus> OPEN_STATUSES = List.copyOf(EnumSet.of(
            InstallmentStatus.OVERDUE,
            InstallmentStatus.DUE,
            InstallmentStatus.PARTIALLY_PAID));

    private final InstallmentRepository installmentRepo;
    private final PaymentAllocationRepository allocationRepo;
    private final LoanAccountRepository loanRepo;
    private final FailedAutoDebitRepository bounceRepo;

    public ApplyPaymentService(
            InstallmentRepository installmentRepo,
            PaymentAllocationRepository allocationRepo,
            LoanAccountRepository loanRepo,
            FailedAutoDebitRepository bounceRepo) {
        this.installmentRepo = installmentRepo;
        this.allocationRepo = allocationRepo;
        this.loanRepo = loanRepo;
        this.bounceRepo = bounceRepo;
    }

    @Transactional
    public List<PaymentAllocation> apply(BankTransaction txn, LoanAccount loan, boolean bounceRecovery) {
        BigDecimal left = txn.getAmount();
        List<PaymentAllocation> created = new ArrayList<>();

        for (Installment inst : installmentRepo.findOpenByLoan(loan.getId(), OPEN_STATUSES)) {
            if (left.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            BigDecimal need = inst.remaining();
            if (need.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal take = left.min(need);
            inst.setAmountPaid(inst.getAmountPaid().add(take));
            inst.setStatus(inst.remaining().compareTo(BigDecimal.ZERO) == 0
                    ? InstallmentStatus.PAID
                    : InstallmentStatus.PARTIALLY_PAID);
            installmentRepo.save(inst);

            created.add(saveAlloc(
                    txn.getId(),
                    loan.getId(),
                    inst.getId(),
                    take,
                    bounceRecovery ? AllocationType.BOUNCE_RECOVERY : AllocationType.INSTALLMENT));
            left = left.subtract(take);
        }

        BigDecimal outstanding = loan.getOutstandingPrincipal().subtract(txn.getAmount()).max(BigDecimal.ZERO);
        loan.setOutstandingPrincipal(outstanding);
        if (outstanding.compareTo(BigDecimal.ZERO) == 0) {
            loan.setStatus(LoanStatus.CLOSED);
        }
        loanRepo.save(loan);

        if (left.compareTo(BigDecimal.ZERO) > 0) {
            created.add(saveAlloc(
                    txn.getId(),
                    loan.getId(),
                    null,
                    left,
                    outstanding.compareTo(BigDecimal.ZERO) == 0
                            ? AllocationType.PREPAYMENT
                            : AllocationType.ADVANCE));
        }

        if (bounceRecovery) {
            bounceRepo.findByLoanAccountIdAndRecoveredFalse(loan.getId()).forEach(b -> {
                b.setRecovered(true);
                bounceRepo.save(b);
            });
        }
        return created;
    }

    private PaymentAllocation saveAlloc(
            Long txnId, Long loanId, Long installmentId, BigDecimal amount, AllocationType type) {
        PaymentAllocation a = new PaymentAllocation();
        a.setBankTransactionId(txnId);
        a.setLoanAccountId(loanId);
        a.setInstallmentId(installmentId);
        a.setAmount(amount);
        a.setAllocationType(type);
        return allocationRepo.save(a);
    }
}
