package com.zenalyst.recon.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "recon")
public class AppSettings {

    private int autoMatchThreshold = 70;
    private int reviewThreshold = 40;
    private int bounceRecoveryDays = 5;
    private int graceDaysBeforeDefault = 7;

    public int getAutoMatchThreshold() {
        return autoMatchThreshold;
    }

    public void setAutoMatchThreshold(int autoMatchThreshold) {
        this.autoMatchThreshold = autoMatchThreshold;
    }

    public int getReviewThreshold() {
        return reviewThreshold;
    }

    public void setReviewThreshold(int reviewThreshold) {
        this.reviewThreshold = reviewThreshold;
    }

    public int getBounceRecoveryDays() {
        return bounceRecoveryDays;
    }

    public void setBounceRecoveryDays(int bounceRecoveryDays) {
        this.bounceRecoveryDays = bounceRecoveryDays;
    }

    public int getGraceDaysBeforeDefault() {
        return graceDaysBeforeDefault;
    }

    public void setGraceDaysBeforeDefault(int graceDaysBeforeDefault) {
        this.graceDaysBeforeDefault = graceDaysBeforeDefault;
    }
}
