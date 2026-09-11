package com.zenalyst.recon.service;

import com.zenalyst.recon.config.AppSettings;
import com.zenalyst.recon.dto.DefaultNoticeDto;
import com.zenalyst.recon.dto.WhoPaidDto;
import com.zenalyst.recon.entity.BankTransaction;
import com.zenalyst.recon.enums.BankTxnStatus;
import com.zenalyst.recon.entity.Installment;
import com.zenalyst.recon.entity.LoanAccount;
import com.zenalyst.recon.entity.PaymentAllocation;
import com.zenalyst.recon.repository.BankTransactionRepository;
import com.zenalyst.recon.repository.FailedAutoDebitRepository;
import com.zenalyst.recon.repository.InstallmentRepository;
import com.zenalyst.recon.repository.LoanAccountRepository;
import com.zenalyst.recon.repository.PaymentAllocationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class LoanPaymentService {

    private final LoanAccountRepository loanRepo;
    private final InstallmentRepository installmentRepo;
    private final BankTransactionRepository bankTxnRepo;
    private final PaymentAllocationRepository allocationRepo;
    private final FailedAutoDebitRepository bounceRepo;
    private final AppSettings settings;

    public LoanPaymentService(
            LoanAccountRepository loanRepo,
            InstallmentRepository installmentRepo,
            BankTransactionRepository bankTxnRepo,
            PaymentAllocationRepository allocationRepo,
            FailedAutoDebitRepository bounceRepo,
            AppSettings settings) {
        this.loanRepo = loanRepo;
        this.installmentRepo = installmentRepo;
        this.bankTxnRepo = bankTxnRepo;
        this.allocationRepo = allocationRepo;
        this.bounceRepo = bounceRepo;
        this.settings = settings;
    }

    @Transactional(readOnly = true)
    public WhoPaidDto getWhoPaid(Long loanId) {
        LoanAccount loan = findLoan(loanId);

        WhoPaidDto dto = new WhoPaidDto();
        dto.setLoanId(loan.getId());
        dto.setLoanNumber(loan.getLoanAccountNumber());
        dto.setBorrowerName(loan.getBorrower().getFullName());
        dto.setBorrowerCode(loan.getBorrower().getBorrowerCode());

        BigDecimal total = BigDecimal.ZERO;
        for (BankTransaction t : bankTxnRepo.findByMatchedLoanId(loanId)) {
            if (t.getStatus() != BankTxnStatus.AUTO_MATCHED && t.getStatus() != BankTxnStatus.MANUALLY_MATCHED) {
                continue;
            }
            WhoPaidDto.PaymentItem item = new WhoPaidDto.PaymentItem();
            item.setBankTxnId(t.getId());
            item.setBankReference(t.getBankReference());
            item.setTxnDate(t.getTxnDate());
            item.setAmount(t.getAmount());
            item.setStatus(t.getStatus().name());
            item.setMatchScore(t.getMatchScore());
            item.setMatchReasons(t.getMatchReasons());
            dto.getPayments().add(item);
            total = total.add(t.getAmount());
        }
        dto.setAmountReceived(total);

        List<Installment> open = installmentRepo.findOpenByLoan(loanId, ApplyPaymentService.OPEN_STATUSES);
        if (open.isEmpty()) {
            dto.setInstallmentCovered(true);
            dto.setStatus("PAID");
            return dto;
        }

        Installment next = open.get(0);
        boolean covered = next.remaining().compareTo(BigDecimal.ZERO) == 0;
        dto.setInstallmentCovered(covered);
        if (covered) {
            dto.setStatus("PAID");
        } else if (next.getAmountPaid().compareTo(BigDecimal.ZERO) > 0) {
            dto.setStatus("PARTIAL");
        } else {
            dto.setStatus("UNPAID");
        }
        return dto;
    }

    @Transactional(readOnly = true)
    public List<DefaultNoticeDto> getDefaultList(LocalDate asOf) {
        List<DefaultNoticeDto> list = new ArrayList<>();

        for (Installment inst : installmentRepo.findUnpaidPastDue(asOf)) {
            LoanAccount loan = inst.getLoanAccount();
            long daysPastDue = ChronoUnit.DAYS.between(inst.getDueDate(), asOf);

            DefaultNoticeDto row = new DefaultNoticeDto();
            row.setLoanId(loan.getId());
            row.setLoanNumber(loan.getLoanAccountNumber());
            row.setBorrowerName(loan.getBorrower().getFullName());
            row.setInstallmentId(inst.getId());
            row.setDueDate(inst.getDueDate());
            row.setAmountLeft(inst.remaining());

            boolean bounceOpen = bounceRepo.findByLoanAccountIdAndRecoveredFalse(loan.getId()).stream()
                    .anyMatch(b -> MatchService.withinDays(b.getAttemptDate(), asOf, settings.getBounceRecoveryDays()));

            boolean inReview = bankTxnRepo.findByStatus(BankTxnStatus.NEEDS_REVIEW).stream()
                    .anyMatch(t -> loan.getId().equals(t.getMatchedLoanId()));

            if (daysPastDue < settings.getGraceDaysBeforeDefault()) {
                row.setCanSendDefaultNotice(false);
                row.setReason("inside grace (" + daysPastDue + "d)");
            } else if (bounceOpen) {
                row.setCanSendDefaultNotice(false);
                row.setReason("bounce recovery window open");
            } else if (inReview) {
                row.setCanSendDefaultNotice(false);
                row.setReason("payment waiting in review");
            } else {
                row.setCanSendDefaultNotice(true);
                row.setReason("past grace, no matched payment (" + daysPastDue + "d)");
            }
            list.add(row);
        }
        return list;
    }

    @Transactional(readOnly = true)
    public List<PaymentAllocation> getAllocations(Long loanId) {
        findLoan(loanId);
        return allocationRepo.findByLoanAccountId(loanId);
    }

    private LoanAccount findLoan(Long loanId) {
        return loanRepo.findById(loanId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown loan: " + loanId));
    }
}
