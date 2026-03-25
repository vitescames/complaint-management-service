package com.complaintmanagementservice.adapters.out.messaging;

import com.complaintmanagementservice.application.exception.InfrastructureUnavailableException;
import com.complaintmanagementservice.application.exception.MessagePublishingException;
import com.complaintmanagementservice.application.notification.ComplaintSlaWarningNotification;
import com.complaintmanagementservice.application.port.out.ComplaintCreatedMessagePort;
import com.complaintmanagementservice.application.port.out.ComplaintSlaWarningMessagePort;
import com.complaintmanagementservice.domain.event.ComplaintCreatedDomainEvent;
import com.complaintmanagementservice.infrastructure.config.MessagingProperties;
import com.complaintmanagementservice.infrastructure.resilience.ResilienceProfile;
import com.complaintmanagementservice.infrastructure.resilience.ResilientExecutor;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
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
                complaintMessagePayloadMapper.toComplaintCreatedMessage(event),
                "Nao foi possivel publicar o evento de criacao da reclamacao"
        );
    }

    @Override
    public void publish(ComplaintSlaWarningNotification notification) {
        publishMessage(
                messagingProperties.queues().complaintSlaWarning(),
                complaintMessagePayloadMapper.toSlaWarningMessage(notification),
                "Nao foi possivel publicar o aviso de SLA da reclamacao"
        );
    }

    private void publishMessage(String queueName, Object payload, String messageOnFailure) {
        try {
            resilientExecutor.executeRunnable(
                    ResilienceProfile.MESSAGING,
                    () -> jmsTemplate.convertAndSend(queueName, payload)
            );
        }
        catch (CallNotPermittedException exception) {
            throw new InfrastructureUnavailableException("A infraestrutura de mensageria esta temporariamente indisponivel", exception);
        }
        catch (RuntimeException exception) {
            throw new MessagePublishingException(messageOnFailure, exception);
        }
    }
}
