package com.complaintmanagementservice.application.usecase;

import com.complaintmanagementservice.TestFixtures;
import com.complaintmanagementservice.application.notification.ComplaintSlaWarningNotification;
import com.complaintmanagementservice.application.port.out.ComplaintRepositoryPort;
import com.complaintmanagementservice.application.port.out.ComplaintSlaWarningMessagePort;
import com.complaintmanagementservice.domain.model.Complaint;
import com.complaintmanagementservice.domain.model.ComplaintStatus;
import com.complaintmanagementservice.domain.service.ComplaintSlaPolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublishSlaWarningsUseCaseImplTest {

    @Mock
    private ComplaintRepositoryPort complaintRepositoryPort;

    @Mock
    private ComplaintSlaWarningMessagePort complaintSlaWarningMessagePort;

    @Test
    void shouldPublishOnlyDueSlaWarnings() {
        Clock fixedClock = Clock.fixed(Instant.parse("2026-03-23T07:00:00Z"), ZoneOffset.UTC);
        PublishSlaWarningsUseCaseImpl useCase = new PublishSlaWarningsUseCaseImpl(
                complaintRepositoryPort,
                complaintSlaWarningMessagePort,
                new ComplaintSlaPolicy(),
                fixedClock
        );
        Complaint dueComplaint = TestFixtures.approachingSlaComplaint();
        Complaint notDueComplaint = Complaint.builder()
                .id(dueComplaint.id())
                .customer(dueComplaint.customer())
                .complaintDate(LocalDate.of(2026, 3, 15))
                .complaintText(dueComplaint.complaintText())
                .documentUrls(dueComplaint.documentUrls())
                .status(ComplaintStatus.PROCESSING)
                .categories(dueComplaint.categories())
                .registeredAt(dueComplaint.registeredAt())
                .buildReconstituted();
        when(complaintRepositoryPort.findNonResolvedComplaintsCreatedOn(LocalDate.of(2026, 3, 16)))
                .thenReturn(List.of(dueComplaint, notDueComplaint));

        useCase.publishWarnings();

        ArgumentCaptor<ComplaintSlaWarningNotification> captor =
                ArgumentCaptor.forClass(ComplaintSlaWarningNotification.class);
        verify(complaintSlaWarningMessagePort).publish(captor.capture());
        assertThat(captor.getValue().complaintId()).isEqualTo(dueComplaint.id());
        assertThat(captor.getValue().slaDeadlineDate()).isEqualTo(LocalDate.of(2026, 3, 26));
    }

    @Test
    void shouldSkipSlaPublicationWhenRepositoryReturnsNothing() {
        PublishSlaWarningsUseCaseImpl useCase = new PublishSlaWarningsUseCaseImpl(
                complaintRepositoryPort,
                complaintSlaWarningMessagePort,
                new ComplaintSlaPolicy(),
                Clock.fixed(Instant.parse("2026-03-23T07:00:00Z"), ZoneOffset.UTC)
        );
        when(complaintRepositoryPort.findNonResolvedComplaintsCreatedOn(LocalDate.of(2026, 3, 16))).thenReturn(List.of());

        useCase.publishWarnings();

        verify(complaintSlaWarningMessagePort, never()).publish(any());
    }
}
