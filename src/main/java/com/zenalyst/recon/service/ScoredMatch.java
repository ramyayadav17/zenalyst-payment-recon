package com.zenalyst.recon.service;

import com.zenalyst.recon.entity.LoanAccount;
import com.zenalyst.recon.enums.MatchDecision;

import java.util.ArrayList;
import java.util.List;

public class ScoredMatch {

    private final LoanAccount loan;
    private int score;
    private final List<String> reasons = new ArrayList<>();
    private MatchDecision decision;
    private boolean bounceRecovery;

    public ScoredMatch(LoanAccount loan) {
        this.loan = loan;
    }

    public void addPoints(int points, String reason) {
        if (points <= 0) {
            return;
        }
        score += points;
        reasons.add(reason + " (+" + points + ")");
    }

    public void note(String reason) {
        reasons.add(reason);
    }

    public LoanAccount getLoan() {
        return loan;
    }

    public int getScore() {
        return score;
    }

    public List<String> getReasons() {
        return reasons;
    }

    public MatchDecision getDecision() {
        return decision;
    }

    public void setDecision(MatchDecision decision) {
        this.decision = decision;
    }

    public boolean isBounceRecovery() {
        return bounceRecovery;
    }

    public void setBounceRecovery(boolean bounceRecovery) {
        this.bounceRecovery = bounceRecovery;
    }

    public String reasonText() {
        return String.join("; ", reasons);
    }
}
