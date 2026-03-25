package com.complaintmanagementservice.adapters.out.messaging;

import com.complaintmanagementservice.TestFixtures;
import com.complaintmanagementservice.infrastructure.config.MessagingProperties;
import com.complaintmanagementservice.infrastructure.resilience.ResilienceProfile;
import com.complaintmanagementservice.infrastructure.resilience.ResilientExecutor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jms.core.JmsTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
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

        assertThat(mapper.toComplaintCreatedMessage(TestFixtures.complaintCreatedNotification()).complaintId())
                .isEqualTo(TestFixtures.complaint().id().value());
        assertThat(mapper.toSlaWarningMessage(TestFixtures.complaintSlaWarningNotification()).slaDeadlineDate())
                .isEqualTo(TestFixtures.complaintSlaWarningNotification().slaDeadlineDate());
    }

    @Test
    void shouldPublishMessagesToQueuesThroughResilientExecutor() {
        MessagingProperties properties = new MessagingProperties(
                "broker",
                new MessagingProperties.QueueProperties("received", "created", "warning"),
                new MessagingProperties.RedeliveryProperties(3, 250, 2.0)
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

        adapter.publish(TestFixtures.complaintCreatedNotification());
        adapter.publish(TestFixtures.complaintSlaWarningNotification());

        ArgumentCaptor<String> destinationCaptor = ArgumentCaptor.forClass(String.class);
        verify(resilientExecutor, org.mockito.Mockito.times(2))
                .executeRunnable(eq(ResilienceProfile.MESSAGING), any(Runnable.class));
        verify(jmsTemplate, org.mockito.Mockito.times(2))
                .convertAndSend(destinationCaptor.capture(), org.mockito.ArgumentMatchers.<Object>any());
        assertThat(destinationCaptor.getAllValues()).containsExactly("created", "warning");
    }
}
