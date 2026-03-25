package com.complaintmanagementservice.application.usecase;

import com.complaintmanagementservice.TestFixtures;
import com.complaintmanagementservice.application.port.out.ComplaintCreatedMessagePort;
import com.complaintmanagementservice.application.port.out.ComplaintRepositoryPort;
import com.complaintmanagementservice.application.port.out.ComplaintSlaWarningMessagePort;
import com.complaintmanagementservice.domain.event.ComplaintCreatedDomainEvent;
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
class SearchAndSlaUseCasesTest {

    @Mock
    private ComplaintRepositoryPort complaintRepositoryPort;

    @Mock
    private ComplaintCreatedMessagePort complaintCreatedMessagePort;

    @Mock
    private ComplaintSlaWarningMessagePort complaintSlaWarningMessagePort;

    @Test
    void shouldDelegateSearchToRepository() {
        SearchComplaintsService service = new SearchComplaintsService(complaintRepositoryPort);
        when(complaintRepositoryPort.search(TestFixtures.searchQuery())).thenReturn(List.of(TestFixtures.complaint()));

        List<Complaint> complaints = service.search(TestFixtures.searchQuery());

        assertThat(complaints).extracting(Complaint::id).containsExactly(TestFixtures.complaint().id());
    }

    @Test
    void shouldPublishCreatedMessageFromDomainEvent() {
        ComplaintCreatedDomainEventHandler handler = new ComplaintCreatedDomainEventHandler(complaintCreatedMessagePort);
        ComplaintCreatedDomainEvent event = TestFixtures.complaintCreatedDomainEvent();

        handler.handle(event);

        ArgumentCaptor<com.complaintmanagementservice.application.model.ComplaintCreatedNotification> captor =
                ArgumentCaptor.forClass(com.complaintmanagementservice.application.model.ComplaintCreatedNotification.class);
        verify(complaintCreatedMessagePort).publish(captor.capture());
        assertThat(captor.getValue().complaintId()).isEqualTo(event.complaintId());
        assertThat(captor.getValue().createdAt()).isEqualTo(event.occurredAt());
    }

    @Test
    void shouldPublishOnlyDueSlaWarnings() {
        Clock fixedClock = Clock.fixed(Instant.parse("2026-03-23T07:00:00Z"), ZoneOffset.UTC);
        PublishSlaWarningsService service = new PublishSlaWarningsService(
                complaintRepositoryPort,
                complaintSlaWarningMessagePort,
                new ComplaintSlaPolicy(),
                fixedClock
        );
        Complaint dueComplaint = TestFixtures.approachingSlaComplaint();
        Complaint notDueComplaint = Complaint.restore(
                dueComplaint.id(),
                dueComplaint.customer(),
                LocalDate.of(2026, 3, 15),
                dueComplaint.complaintText(),
                dueComplaint.documentUrls(),
                ComplaintStatus.PROCESSING,
                dueComplaint.categories(),
                dueComplaint.registeredAt()
        );
        when(complaintRepositoryPort.findNonResolvedComplaintsCreatedOn(LocalDate.of(2026, 3, 16)))
                .thenReturn(List.of(dueComplaint, notDueComplaint));

        service.publishWarnings();

        ArgumentCaptor<com.complaintmanagementservice.application.model.ComplaintSlaWarningNotification> captor =
                ArgumentCaptor.forClass(com.complaintmanagementservice.application.model.ComplaintSlaWarningNotification.class);
        verify(complaintSlaWarningMessagePort).publish(captor.capture());
        assertThat(captor.getValue().complaintId()).isEqualTo(dueComplaint.id());
        assertThat(captor.getValue().slaDeadlineDate()).isEqualTo(LocalDate.of(2026, 3, 26));
    }

    @Test
    void shouldSkipSlaPublicationWhenRepositoryReturnsNothing() {
        PublishSlaWarningsService service = new PublishSlaWarningsService(
                complaintRepositoryPort,
                complaintSlaWarningMessagePort,
                new ComplaintSlaPolicy(),
                Clock.fixed(Instant.parse("2026-03-23T07:00:00Z"), ZoneOffset.UTC)
        );
        when(complaintRepositoryPort.findNonResolvedComplaintsCreatedOn(LocalDate.of(2026, 3, 16))).thenReturn(List.of());

        service.publishWarnings();

        verify(complaintSlaWarningMessagePort, never()).publish(any());
    }
}
