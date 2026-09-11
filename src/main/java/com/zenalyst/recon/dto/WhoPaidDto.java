package com.zenalyst.recon.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class WhoPaidDto {

    private Long loanId;
    private String loanNumber;
    private String borrowerName;
    private String borrowerCode;
    private boolean installmentCovered;
    private String status;
    private BigDecimal amountReceived;
    private List<PaymentItem> payments = new ArrayList<>();

    public static class PaymentItem {
        private Long bankTxnId;
        private String bankReference;
        private LocalDate txnDate;
        private BigDecimal amount;
        private String status;
        private Integer matchScore;
        private String matchReasons;

        public Long getBankTxnId() {
            return bankTxnId;
        }

        public void setBankTxnId(Long bankTxnId) {
            this.bankTxnId = bankTxnId;
        }

        public String getBankReference() {
            return bankReference;
        }

        public void setBankReference(String bankReference) {
            this.bankReference = bankReference;
        }

        public LocalDate getTxnDate() {
            return txnDate;
        }

        public void setTxnDate(LocalDate txnDate) {
            this.txnDate = txnDate;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public Integer getMatchScore() {
            return matchScore;
        }

        public void setMatchScore(Integer matchScore) {
            this.matchScore = matchScore;
        }

        public String getMatchReasons() {
            return matchReasons;
        }

        public void setMatchReasons(String matchReasons) {
            this.matchReasons = matchReasons;
        }
    }

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

    public String getBorrowerCode() {
        return borrowerCode;
    }

    public void setBorrowerCode(String borrowerCode) {
        this.borrowerCode = borrowerCode;
    }

    public boolean isInstallmentCovered() {
        return installmentCovered;
    }

    public void setInstallmentCovered(boolean installmentCovered) {
        this.installmentCovered = installmentCovered;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getAmountReceived() {
        return amountReceived;
    }

    public void setAmountReceived(BigDecimal amountReceived) {
        this.amountReceived = amountReceived;
    }

    public List<PaymentItem> getPayments() {
        return payments;
    }

    public void setPayments(List<PaymentItem> payments) {
        this.payments = payments;
    }
}
