package com.complaintmanagementservice.adapters.out.messaging;

import com.complaintmanagementservice.application.model.ComplaintCreatedNotification;
import com.complaintmanagementservice.application.model.ComplaintSlaWarningNotification;
import com.complaintmanagementservice.application.port.out.ComplaintCreatedMessagePort;
import com.complaintmanagementservice.application.port.out.ComplaintSlaWarningMessagePort;
import com.complaintmanagementservice.infrastructure.config.MessagingProperties;
import com.complaintmanagementservice.infrastructure.resilience.ResilienceProfile;
import com.complaintmanagementservice.infrastructure.resilience.ResilientExecutor;
import org.springframework.jms.core.JmsTemplate;

public class ComplaintQueuePublisherAdapter implements ComplaintCreatedMessagePort, ComplaintSlaWarningMessagePort {

    private final JmsTemplate jmsTemplate;
    private final MessagingProperties messagingProperties;
    private final ComplaintMessagePayloadMapper complaintMessagePayloadMapper;
    private final ResilientExecutor resilientExecutor;

    public ComplaintQueuePublisherAdapter(
            JmsTemplate jmsTemplate,
            MessagingProperties messagingProperties,
            ComplaintMessagePayloadMapper complaintMessagePayloadMapper,
            ResilientExecutor resilientExecutor
    ) {
        this.jmsTemplate = jmsTemplate;
        this.messagingProperties = messagingProperties;
        this.complaintMessagePayloadMapper = complaintMessagePayloadMapper;
        this.resilientExecutor = resilientExecutor;
    }

    @Override
    public void publish(ComplaintCreatedNotification notification) {
        resilientExecutor.executeRunnable(
                ResilienceProfile.MESSAGING,
                () -> jmsTemplate.convertAndSend(
                        messagingProperties.queues().complaintCreated(),
                        complaintMessagePayloadMapper.toComplaintCreatedMessage(notification)
                )
        );
    }

    @Override
    public void publish(ComplaintSlaWarningNotification notification) {
        resilientExecutor.executeRunnable(
                ResilienceProfile.MESSAGING,
                () -> jmsTemplate.convertAndSend(
                        messagingProperties.queues().complaintSlaWarning(),
                        complaintMessagePayloadMapper.toSlaWarningMessage(notification)
                )
        );
    }
}
