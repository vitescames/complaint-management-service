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
class SearchAndSlaUseCasesTest {

    @Mock
    private ComplaintRepositoryPort complaintRepositoryPort;

    @Mock
    private ComplaintSlaWarningMessagePort complaintSlaWarningMessagePort;

    @Test
    void shouldDelegateSearchToRepository() {
        SearchComplaintsUseCaseImpl useCase = new SearchComplaintsUseCaseImpl(complaintRepositoryPort);
        when(complaintRepositoryPort.search(any())).thenReturn(List.of(TestFixtures.complaint()));

        List<Complaint> complaints = useCase.search(TestFixtures.searchQuery());

        assertThat(complaints).extracting(Complaint::id).containsExactly(TestFixtures.complaint().id());
    }

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
        Complaint notDueComplaint = Complaint.reconstitute(
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
