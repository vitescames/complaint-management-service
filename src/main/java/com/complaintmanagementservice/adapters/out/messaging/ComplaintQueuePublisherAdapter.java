package com.complaintmanagementservice.adapters.out.messaging;

import com.complaintmanagementservice.application.port.out.ComplaintCreatedMessagePort;
import com.complaintmanagementservice.application.port.out.ComplaintSlaWarningMessagePort;
import com.complaintmanagementservice.domain.event.ComplaintCreatedDomainEvent;
import com.complaintmanagementservice.domain.event.ComplaintSlaWarningTriggeredDomainEvent;
import com.complaintmanagementservice.infrastructure.config.MessagingProperties;
import com.complaintmanagementservice.infrastructure.resilience.ResilienceProfile;
import com.complaintmanagementservice.infrastructure.resilience.ResilientExecutor;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

@Component
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
    public void publish(ComplaintCreatedDomainEvent event) {
        publishMessage(
                messagingProperties.queues().complaintCreated(),
                complaintMessagePayloadMapper.toComplaintCreatedMessage(event)
        );
    }

    @Override
    public void publish(ComplaintSlaWarningTriggeredDomainEvent event) {
        publishMessage(
                messagingProperties.queues().complaintSlaWarning(),
                complaintMessagePayloadMapper.toSlaWarningMessage(event)
        );
    }

    private void publishMessage(String queueName, Object payload) {
        resilientExecutor.executeRunnable(
                ResilienceProfile.MESSAGING,
                () -> jmsTemplate.convertAndSend(queueName, payload)
        );
    }
}
