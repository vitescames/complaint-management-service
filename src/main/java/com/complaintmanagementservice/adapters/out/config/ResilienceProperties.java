package com.complaintmanagementservice.adapters.out.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "application.resilience")
public record ResilienceProperties(ProfileSettings persistence, ProfileSettings messaging) {

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
