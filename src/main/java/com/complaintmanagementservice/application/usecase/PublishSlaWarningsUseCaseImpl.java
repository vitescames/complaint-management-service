package com.complaintmanagementservice.application.usecase;

import com.complaintmanagementservice.application.event.DomainEventPublisher;
import com.complaintmanagementservice.application.port.in.PublishSlaWarningsUseCase;
import com.complaintmanagementservice.application.port.out.ComplaintRepositoryPort;
import com.complaintmanagementservice.domain.event.ComplaintSlaWarningTriggeredDomainEvent;
import com.complaintmanagementservice.domain.model.Complaint;
import com.complaintmanagementservice.domain.service.ComplaintSlaPolicy;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public class PublishSlaWarningsUseCaseImpl implements PublishSlaWarningsUseCase {

    private final ComplaintRepositoryPort complaintRepositoryPort;
    private final DomainEventPublisher domainEventPublisher;
    private final ComplaintSlaPolicy complaintSlaPolicy;
    private final Clock clock;

    public PublishSlaWarningsUseCaseImpl(
            ComplaintRepositoryPort complaintRepositoryPort,
            DomainEventPublisher domainEventPublisher,
            ComplaintSlaPolicy complaintSlaPolicy,
            Clock clock
    ) {
        this.complaintRepositoryPort = complaintRepositoryPort;
        this.domainEventPublisher = domainEventPublisher;
        this.complaintSlaPolicy = complaintSlaPolicy;
        this.clock = clock;
    }

    @Override
    public void publishWarnings() {
        LocalDate referenceDate = LocalDate.now(clock);
        LocalDate complaintDate = complaintSlaPolicy.warningTriggerComplaintDate(referenceDate);
        Instant occurredAt = Instant.now(clock);
        List<Complaint> complaints = complaintRepositoryPort.findNonResolvedComplaintsByComplaintDate(complaintDate);
        complaints.stream()
                .filter(complaint -> complaintSlaPolicy.isWarningDue(complaint, referenceDate))
                .map(complaint -> new ComplaintSlaWarningTriggeredDomainEvent(
                        complaint.id(),
                        complaintSlaPolicy.deadlineFor(complaint),
                        occurredAt
                ))
                .forEach(domainEventPublisher::publish);
    }
}
