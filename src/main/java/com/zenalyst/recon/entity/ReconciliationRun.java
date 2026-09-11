package com.zenalyst.recon.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "reconciliation_runs")
public class ReconciliationRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate businessDate;

    private int processed;
    private int autoMatched;
    private int needsReview;
    private int unmatched;

    private Instant startedAt = Instant.now();
    private Instant finishedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDate getBusinessDate() { return businessDate; }
    public void setBusinessDate(LocalDate businessDate) { this.businessDate = businessDate; }
    public int getProcessed() { return processed; }
    public void setProcessed(int processed) { this.processed = processed; }
    public int getAutoMatched() { return autoMatched; }
    public void setAutoMatched(int autoMatched) { this.autoMatched = autoMatched; }
    public int getNeedsReview() { return needsReview; }
    public void setNeedsReview(int needsReview) { this.needsReview = needsReview; }
    public int getUnmatched() { return unmatched; }
    public void setUnmatched(int unmatched) { this.unmatched = unmatched; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getFinishedAt() { return finishedAt; }
    public void setFinishedAt(Instant finishedAt) { this.finishedAt = finishedAt; }
}
