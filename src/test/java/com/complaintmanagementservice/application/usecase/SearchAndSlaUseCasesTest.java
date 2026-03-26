package com.complaintmanagementservice.application.usecase;

import com.complaintmanagementservice.TestFixtures;
import com.complaintmanagementservice.application.notification.ComplaintSlaWarningNotification;
import com.complaintmanagementservice.application.port.out.ComplaintRepositoryPort;
import com.complaintmanagementservice.application.port.out.ComplaintSlaWarningMessagePort;
import com.complaintmanagementservice.application.query.SearchComplaintsQuery;
import com.complaintmanagementservice.domain.exception.DomainValidationException;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    void shouldValidateAndDelegateSearchToRepository() {
        SearchComplaintsUseCaseImpl useCase = new SearchComplaintsUseCaseImpl(complaintRepositoryPort);
        when(complaintRepositoryPort.search(any())).thenReturn(List.of(TestFixtures.complaint()));
        SearchComplaintsQuery query = TestFixtures.searchQuery();

        List<Complaint> complaints = useCase.search(query);

        ArgumentCaptor<SearchComplaintsQuery> captor = ArgumentCaptor.forClass(SearchComplaintsQuery.class);
        verify(complaintRepositoryPort).search(captor.capture());
        assertThat(captor.getValue()).isSameAs(query);
        assertThat(captor.getValue().customerCpf()).isEqualTo("52998224725");
        assertThat(captor.getValue().categoryNames()).containsExactly("acesso", "cobranca");
        assertThat(captor.getValue().statusIds()).containsExactly(ComplaintStatus.PENDING.id());
        assertThat(complaints).extracting(Complaint::id).containsExactly(TestFixtures.complaint().id());
    }

    @Test
    void shouldDelegateSearchWithEmptyFilters() {
        SearchComplaintsUseCaseImpl useCase = new SearchComplaintsUseCaseImpl(complaintRepositoryPort);
        when(complaintRepositoryPort.search(any())).thenReturn(List.of());

        useCase.search(SearchComplaintsQuery.builder().build());

        ArgumentCaptor<SearchComplaintsQuery> captor = ArgumentCaptor.forClass(SearchComplaintsQuery.class);
        verify(complaintRepositoryPort).search(captor.capture());
        assertThat(captor.getValue().customerCpf()).isNull();
        assertThat(captor.getValue().categoryNames()).isEmpty();
        assertThat(captor.getValue().statusIds()).isEmpty();
        assertThat(captor.getValue().startDate()).isNull();
        assertThat(captor.getValue().endDate()).isNull();
    }

    @Test
    void shouldAllowPartialDateFilters() {
        SearchComplaintsUseCaseImpl useCase = new SearchComplaintsUseCaseImpl(complaintRepositoryPort);
        when(complaintRepositoryPort.search(any())).thenReturn(List.of());
        SearchComplaintsQuery query = SearchComplaintsQuery.builder()
                .startDate(LocalDate.of(2026, 3, 1))
                .build();

        useCase.search(query);

        ArgumentCaptor<SearchComplaintsQuery> captor = ArgumentCaptor.forClass(SearchComplaintsQuery.class);
        verify(complaintRepositoryPort).search(captor.capture());
        SearchComplaintsQuery capturedQuery = captor.getValue();
        assertThat(capturedQuery).isSameAs(query);
        assertThat(capturedQuery.customerCpf()).isNull();
        assertThat(capturedQuery.categoryNames()).isEmpty();
        assertThat(capturedQuery.startDate()).isEqualTo(LocalDate.of(2026, 3, 1));
        assertThat(capturedQuery.endDate()).isNull();
    }

    @Test
    void shouldRejectInvalidSearchCriteria() {
        SearchComplaintsUseCaseImpl useCase = new SearchComplaintsUseCaseImpl(complaintRepositoryPort);

        SearchComplaintsQuery invalidCpfQuery = SearchComplaintsQuery.builder()
                .customerCpf("invalid")
                .build();
        SearchComplaintsQuery invalidStatusQuery = SearchComplaintsQuery.builder()
                .statusIds(List.of(99))
                .build();

        assertThatThrownBy(() -> useCase.search(invalidCpfQuery))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("CPF inválido.");
        assertThatThrownBy(() -> useCase.search(invalidStatusQuery))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("O status da reclamação é inválido.");
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
