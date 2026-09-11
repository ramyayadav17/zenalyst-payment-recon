package com.zenalyst.recon.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class DefaultNoticeDto {

    private Long loanId;
    private String loanNumber;
    private String borrowerName;
    private Long installmentId;
    private LocalDate dueDate;
    private BigDecimal amountLeft;
    private boolean canSendDefaultNotice;
    private String reason;

    public Long getLoanId() {
        return loanId;
    }

    public void setLoanId(Long loanId) {
        this.loanId = loanId;
    }

    public String getLoanNumber() {
        return loanNumber;
    }

    public void setLoanNumber(String loanNumber) {
        this.loanNumber = loanNumber;
    }

    public String getBorrowerName() {
        return borrowerName;
    }

    public void setBorrowerName(String borrowerName) {
        this.borrowerName = borrowerName;
    }

    public Long getInstallmentId() {
        return installmentId;
    }

    public void setInstallmentId(Long installmentId) {
        this.installmentId = installmentId;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public BigDecimal getAmountLeft() {
        return amountLeft;
    }

    public void setAmountLeft(BigDecimal amountLeft) {
        this.amountLeft = amountLeft;
    }

    public boolean isCanSendDefaultNotice() {
        return canSendDefaultNotice;
    }

    public void setCanSendDefaultNotice(boolean canSendDefaultNotice) {
        this.canSendDefaultNotice = canSendDefaultNotice;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
