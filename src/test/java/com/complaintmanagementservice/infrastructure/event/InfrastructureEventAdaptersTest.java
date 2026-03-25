package com.complaintmanagementservice.infrastructure.event;

import com.complaintmanagementservice.TestFixtures;
import com.complaintmanagementservice.application.usecase.ComplaintCreatedDomainEventHandler;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class InfrastructureEventAdaptersTest {

    @Test
    void shouldPublishAndObserveSpringEvents() {
        ApplicationEventPublisher applicationEventPublisher = mock(ApplicationEventPublisher.class);
        SpringDomainEventPublisherAdapter publisherAdapter = new SpringDomainEventPublisherAdapter(applicationEventPublisher);
        ComplaintCreatedDomainEventHandler handler = mock(ComplaintCreatedDomainEventHandler.class);
        SpringComplaintCreatedEventObserver observer = new SpringComplaintCreatedEventObserver(handler);

        publisherAdapter.publish(TestFixtures.complaintCreatedDomainEvent());
        observer.onComplaintCreated(TestFixtures.complaintCreatedDomainEvent());

        verify(applicationEventPublisher).publishEvent(TestFixtures.complaintCreatedDomainEvent());
        verify(handler).handle(TestFixtures.complaintCreatedDomainEvent());
    }
}
