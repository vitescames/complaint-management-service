package com.complaintmanagementservice.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "application.messaging")
public record MessagingProperties(
        String brokerName,
        QueueProperties queues,
        RedeliveryProperties redelivery
) {

    public record QueueProperties(
            String complaintReceived,
            String complaintCreated,
            String complaintSlaWarning
    ) {
    }

    public record RedeliveryProperties(
            int maximumRedeliveries,
            long initialDelayMillis,
            double backoffMultiplier
    ) {
    }
}
