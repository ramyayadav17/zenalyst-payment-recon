package com.zenalyst.recon.entity;

import com.zenalyst.recon.enums.BankTxnStatus;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "bank_transactions", indexes = {
        @Index(name = "idx_bank_txn_date", columnList = "txnDate"),
        @Index(name = "idx_bank_txn_status", columnList = "status")
})
public class BankTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String bankReference;

    @Column(nullable = false)
    private LocalDate txnDate;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 2000)
    private String narration;

    private String counterpartyAccount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BankTxnStatus status = BankTxnStatus.UNMATCHED;

    private Long matchedLoanId;

    private Integer matchScore;

    @Column(length = 2000)
    private String matchReasons;

    private Instant ingestedAt = Instant.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getBankReference() { return bankReference; }
    public void setBankReference(String bankReference) { this.bankReference = bankReference; }
    public LocalDate getTxnDate() { return txnDate; }
    public void setTxnDate(LocalDate txnDate) { this.txnDate = txnDate; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getNarration() { return narration; }
    public void setNarration(String narration) { this.narration = narration; }
    public String getCounterpartyAccount() { return counterpartyAccount; }
    public void setCounterpartyAccount(String counterpartyAccount) { this.counterpartyAccount = counterpartyAccount; }
    public BankTxnStatus getStatus() { return status; }
    public void setStatus(BankTxnStatus status) { this.status = status; }
    public Long getMatchedLoanId() { return matchedLoanId; }
    public void setMatchedLoanId(Long matchedLoanId) { this.matchedLoanId = matchedLoanId; }
    public Integer getMatchScore() { return matchScore; }
    public void setMatchScore(Integer matchScore) { this.matchScore = matchScore; }
    public String getMatchReasons() { return matchReasons; }
    public void setMatchReasons(String matchReasons) { this.matchReasons = matchReasons; }
    public Instant getIngestedAt() { return ingestedAt; }
    public void setIngestedAt(Instant ingestedAt) { this.ingestedAt = ingestedAt; }
}
