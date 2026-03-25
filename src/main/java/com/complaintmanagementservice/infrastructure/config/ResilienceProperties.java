package com.complaintmanagementservice.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "application.resilience")
public record ResilienceProperties(Profiles profiles) {

    public record Profiles(ProfileSettings persistence, ProfileSettings messaging) {
    }

    public record ProfileSettings(
            int maxAttempts,
            long waitDurationMillis,
            int slidingWindowSize,
            int minimumNumberOfCalls,
            float failureRateThreshold,
            long waitDurationInOpenStateMillis
    ) {
    }
}
