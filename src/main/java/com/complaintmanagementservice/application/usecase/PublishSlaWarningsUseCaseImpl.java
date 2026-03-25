package com.complaintmanagementservice.application.usecase;

import com.complaintmanagementservice.application.notification.ComplaintSlaWarningNotification;
import com.complaintmanagementservice.application.port.in.PublishSlaWarningsUseCase;
import com.complaintmanagementservice.application.port.out.ComplaintRepositoryPort;
import com.complaintmanagementservice.application.port.out.ComplaintSlaWarningMessagePort;
import com.complaintmanagementservice.domain.model.Complaint;
import com.complaintmanagementservice.domain.service.ComplaintSlaPolicy;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

public class PublishSlaWarningsUseCaseImpl implements PublishSlaWarningsUseCase {

    private final ComplaintRepositoryPort complaintRepositoryPort;
    private final ComplaintSlaWarningMessagePort complaintSlaWarningMessagePort;
    private final ComplaintSlaPolicy complaintSlaPolicy;
    private final Clock clock;

    public PublishSlaWarningsUseCaseImpl(
            ComplaintRepositoryPort complaintRepositoryPort,
            ComplaintSlaWarningMessagePort complaintSlaWarningMessagePort,
            ComplaintSlaPolicy complaintSlaPolicy,
            Clock clock
    ) {
        this.complaintRepositoryPort = complaintRepositoryPort;
        this.complaintSlaWarningMessagePort = complaintSlaWarningMessagePort;
        this.complaintSlaPolicy = complaintSlaPolicy;
        this.clock = clock;
    }

    @Override
    public void publishWarnings() {
        LocalDate referenceDate = LocalDate.now(clock);
        LocalDate complaintDate = complaintSlaPolicy.warningTriggerComplaintDate(referenceDate);
        List<Complaint> complaints = complaintRepositoryPort.findNonResolvedComplaintsCreatedOn(complaintDate);
        complaints.stream()
                .filter(complaint -> complaintSlaPolicy.isWarningDue(complaint, referenceDate))
                .map(complaint -> new ComplaintSlaWarningNotification(complaint.id(), complaintSlaPolicy.deadlineFor(complaint)))
                .forEach(complaintSlaWarningMessagePort::publish);
    }
}
