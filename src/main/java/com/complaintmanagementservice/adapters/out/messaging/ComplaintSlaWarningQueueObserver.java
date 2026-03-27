package com.complaintmanagementservice.adapters.out.messaging;

import com.complaintmanagementservice.application.event.DomainEventObserver;
import com.complaintmanagementservice.application.port.out.ComplaintSlaWarningMessagePort;
import com.complaintmanagementservice.domain.event.ComplaintSlaWarningTriggeredDomainEvent;
import org.springframework.stereotype.Component;

@Component
public class ComplaintSlaWarningQueueObserver implements DomainEventObserver<ComplaintSlaWarningTriggeredDomainEvent> {

    private final ComplaintSlaWarningMessagePort complaintSlaWarningMessagePort;

    public ComplaintSlaWarningQueueObserver(ComplaintSlaWarningMessagePort complaintSlaWarningMessagePort) {
        this.complaintSlaWarningMessagePort = complaintSlaWarningMessagePort;
    }

    @Override
    public Class<ComplaintSlaWarningTriggeredDomainEvent> supportedEventType() {
        return ComplaintSlaWarningTriggeredDomainEvent.class;
    }

    @Override
    public void onEvent(ComplaintSlaWarningTriggeredDomainEvent event) {
        complaintSlaWarningMessagePort.publish(event);
    }
}
