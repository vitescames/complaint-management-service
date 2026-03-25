package com.complaintmanagementservice.application.event;

import com.complaintmanagementservice.TestFixtures;
import com.complaintmanagementservice.adapters.out.messaging.ComplaintCreatedQueueObserver;
import com.complaintmanagementservice.application.exception.RequestValidationException;
import com.complaintmanagementservice.application.port.out.ComplaintCreatedMessagePort;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class ObserverDomainEventPublisherTest {

    @Test
    void shouldNotifyRegisteredObserverAndAllowRemoval() {
        ComplaintCreatedMessagePort messagePort = mock(ComplaintCreatedMessagePort.class);
        ComplaintCreatedQueueObserver observer = new ComplaintCreatedQueueObserver(messagePort);
        ObserverDomainEventPublisher publisher = new ObserverDomainEventPublisher();

        publisher.register(observer);
        publisher.publish(TestFixtures.complaintCreatedDomainEvent());
        publisher.remove(observer);
        publisher.publish(TestFixtures.complaintCreatedDomainEvent());

        verify(messagePort, times(1)).publish(TestFixtures.complaintCreatedDomainEvent());
    }

    @Test
    void shouldRejectNullObserverOrEvent() {
        ObserverDomainEventPublisher publisher = new ObserverDomainEventPublisher();

        assertThatThrownBy(() -> publisher.register(null))
                .isInstanceOf(RequestValidationException.class);
        assertThatThrownBy(() -> publisher.remove(null))
                .isInstanceOf(RequestValidationException.class);
        assertThatThrownBy(() -> publisher.publish(null))
                .isInstanceOf(RequestValidationException.class);
    }
}
