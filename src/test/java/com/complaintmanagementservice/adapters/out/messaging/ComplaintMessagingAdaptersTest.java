package com.complaintmanagementservice.adapters.out.messaging;

import com.complaintmanagementservice.TestFixtures;
import com.complaintmanagementservice.application.exception.InfrastructureUnavailableException;
import com.complaintmanagementservice.application.exception.MessagePublishingException;
import com.complaintmanagementservice.infrastructure.config.MessagingProperties;
import com.complaintmanagementservice.infrastructure.resilience.ResilienceProfile;
import com.complaintmanagementservice.infrastructure.resilience.ResilientExecutor;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jms.core.JmsTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ComplaintMessagingAdaptersTest {

    @Mock
    private JmsTemplate jmsTemplate;

    @Mock
    private ResilientExecutor resilientExecutor;

    @Test
    void shouldMapNotificationsToQueuePayloads() {
        ComplaintMessagePayloadMapper mapper = new ComplaintMessagePayloadMapper();

        assertThat(mapper.toComplaintCreatedMessage(TestFixtures.complaintCreatedDomainEvent()).complaintId())
                .isEqualTo(TestFixtures.complaint().id().value());
        assertThat(mapper.toSlaWarningMessage(TestFixtures.complaintSlaWarningNotification()).slaDeadlineDate())
                .isEqualTo(TestFixtures.complaintSlaWarningNotification().slaDeadlineDate());
    }

    @Test
    void shouldPublishMessagesToQueuesThroughResilientExecutor() {
        MessagingProperties properties = new MessagingProperties(
                "broker",
                new MessagingProperties.QueueNames("received", "created", "warning"),
                new MessagingProperties.RedeliverySettings(3, 250, 2.0)
        );
        ComplaintQueuePublisherAdapter adapter = new ComplaintQueuePublisherAdapter(
                jmsTemplate,
                properties,
                new ComplaintMessagePayloadMapper(),
                resilientExecutor
        );
        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(1);
            runnable.run();
            return null;
        }).when(resilientExecutor).executeRunnable(eq(ResilienceProfile.MESSAGING), any(Runnable.class));

        adapter.publish(TestFixtures.complaintCreatedDomainEvent());
        adapter.publish(TestFixtures.complaintSlaWarningNotification());

        verify(resilientExecutor, org.mockito.Mockito.times(2))
                .executeRunnable(eq(ResilienceProfile.MESSAGING), any(Runnable.class));
        verify(jmsTemplate).convertAndSend(eq("created"), org.mockito.ArgumentMatchers.<Object>any());
        verify(jmsTemplate).convertAndSend(eq("warning"), org.mockito.ArgumentMatchers.<Object>any());
    }

    @Test
    void shouldWrapInfrastructureFailuresWhenPublishingMessages() {
        MessagingProperties properties = new MessagingProperties(
                "broker",
                new MessagingProperties.QueueNames("received", "created", "warning"),
                new MessagingProperties.RedeliverySettings(3, 250, 2.0)
        );
        ComplaintQueuePublisherAdapter adapter = new ComplaintQueuePublisherAdapter(
                jmsTemplate,
                properties,
                new ComplaintMessagePayloadMapper(),
                resilientExecutor
        );
        doThrow(CallNotPermittedException.createCallNotPermittedException(
                io.github.resilience4j.circuitbreaker.CircuitBreaker.ofDefaults("messaging")
        )).when(resilientExecutor).executeRunnable(eq(ResilienceProfile.MESSAGING), any(Runnable.class));

        assertThatThrownBy(() -> adapter.publish(TestFixtures.complaintCreatedDomainEvent()))
                .isInstanceOf(InfrastructureUnavailableException.class);
    }

    @Test
    void shouldWrapGenericPublishingFailures() {
        MessagingProperties properties = new MessagingProperties(
                "broker",
                new MessagingProperties.QueueNames("received", "created", "warning"),
                new MessagingProperties.RedeliverySettings(3, 250, 2.0)
        );
        ComplaintQueuePublisherAdapter adapter = new ComplaintQueuePublisherAdapter(
                jmsTemplate,
                properties,
                new ComplaintMessagePayloadMapper(),
                resilientExecutor
        );
        doThrow(new IllegalStateException("broker down")).when(resilientExecutor)
                .executeRunnable(eq(ResilienceProfile.MESSAGING), any(Runnable.class));

        assertThatThrownBy(() -> adapter.publish(TestFixtures.complaintSlaWarningNotification()))
                .isInstanceOf(MessagePublishingException.class);
    }
}
