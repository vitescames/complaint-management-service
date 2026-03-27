package com.complaintmanagementservice.adapters.out.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "application.messaging")
public record MessagingProperties(
        String brokerName,
        QueueNames queues,
        RedeliverySettings redelivery
) {

    public record QueueNames(
            String complaintReceived,
            String complaintCreated,
            String complaintSlaWarning
    ) {
    }

    public record RedeliverySettings(
            int maximumRedeliveries,
            long initialDelayMillis,
            double backoffMultiplier
    ) {
    }
}
