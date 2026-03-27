package com.complaintmanagementservice.application.event;

import com.complaintmanagementservice.TestFixtures;
import com.complaintmanagementservice.adapters.out.messaging.ComplaintCreatedQueueObserver;
import com.complaintmanagementservice.adapters.out.messaging.ComplaintSlaWarningQueueObserver;
import com.complaintmanagementservice.application.port.out.ComplaintCreatedMessagePort;
import com.complaintmanagementservice.application.port.out.ComplaintSlaWarningMessagePort;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class ObserverDomainEventPublisherTest {

    @Test
    void shouldNotifyRegisteredObserverAndAllowRemoval() {
        ComplaintCreatedMessagePort messagePort = mock(ComplaintCreatedMessagePort.class);
        ComplaintSlaWarningMessagePort slaWarningMessagePort = mock(ComplaintSlaWarningMessagePort.class);
        ComplaintCreatedQueueObserver observer = new ComplaintCreatedQueueObserver(messagePort);
        ComplaintSlaWarningQueueObserver slaObserver = new ComplaintSlaWarningQueueObserver(slaWarningMessagePort);
        ObserverDomainEventPublisher publisher = new ObserverDomainEventPublisher();

        publisher.register(observer);
        publisher.register(slaObserver);
        publisher.publish(TestFixtures.complaintCreatedDomainEvent());
        publisher.publish(TestFixtures.complaintSlaWarningTriggeredDomainEvent());
        publisher.remove(observer);
        publisher.remove(slaObserver);
        publisher.publish(TestFixtures.complaintCreatedDomainEvent());
        publisher.publish(TestFixtures.complaintSlaWarningTriggeredDomainEvent());

        verify(messagePort, times(1)).publish(TestFixtures.complaintCreatedDomainEvent());
        verify(slaWarningMessagePort, times(1)).publish(TestFixtures.complaintSlaWarningTriggeredDomainEvent());
    }

    @Test
    void shouldIgnoreNullObserverAndNullEvent() {
        ComplaintCreatedMessagePort messagePort = mock(ComplaintCreatedMessagePort.class);
        ComplaintCreatedQueueObserver observer = new ComplaintCreatedQueueObserver(messagePort);
        ObserverDomainEventPublisher publisher = new ObserverDomainEventPublisher();

        publisher.register(null);
        publisher.register(observer);
        publisher.publish(null);
        publisher.remove(null);
        publisher.publish(TestFixtures.complaintCreatedDomainEvent());

        verify(messagePort).publish(TestFixtures.complaintCreatedDomainEvent());
    }
}
