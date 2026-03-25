package com.complaintmanagementservice.application.usecase;

import com.complaintmanagementservice.TestFixtures;
import com.complaintmanagementservice.application.exception.BusinessRuleViolationException;
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
import java.util.Arrays;
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
    void shouldNormalizeAndDelegateSearchToRepository() {
        SearchComplaintsUseCaseImpl useCase = new SearchComplaintsUseCaseImpl(complaintRepositoryPort);
        when(complaintRepositoryPort.search(any())).thenReturn(List.of(TestFixtures.complaint()));

        List<Complaint> complaints = useCase.search(SearchComplaintsQuery.builder()
                .customerCpf(" 52998224725 ")
                .categoryNames(Arrays.asList(" acesso ", null, "cobranca"))
                .statusIds(Arrays.asList(null, ComplaintStatus.PENDING.id()))
                .startDate(LocalDate.of(2026, 3, 1))
                .endDate(LocalDate.of(2026, 3, 31))
                .build());

        ArgumentCaptor<SearchComplaintsQuery> captor = ArgumentCaptor.forClass(SearchComplaintsQuery.class);
        verify(complaintRepositoryPort).search(captor.capture());
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
    void shouldNormalizeBlankValuesAndAllowPartialDateFilters() {
        SearchComplaintsUseCaseImpl useCase = new SearchComplaintsUseCaseImpl(complaintRepositoryPort);
        when(complaintRepositoryPort.search(any())).thenReturn(List.of());

        useCase.search(SearchComplaintsQuery.builder()
                .customerCpf(" ")
                .categoryNames(List.of(" "))
                .startDate(LocalDate.of(2026, 3, 1))
                .build());

        ArgumentCaptor<SearchComplaintsQuery> captor = ArgumentCaptor.forClass(SearchComplaintsQuery.class);
        verify(complaintRepositoryPort).search(captor.capture());
        SearchComplaintsQuery normalizedQuery = captor.getValue();
        assertThat(normalizedQuery.customerCpf()).isNull();
        assertThat(normalizedQuery.categoryNames()).isEmpty();
        assertThat(normalizedQuery.startDate()).isEqualTo(LocalDate.of(2026, 3, 1));
        assertThat(normalizedQuery.endDate()).isNull();
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
        SearchComplaintsQuery invalidDateRangeQuery = SearchComplaintsQuery.builder()
                .startDate(LocalDate.of(2026, 4, 1))
                .endDate(LocalDate.of(2026, 3, 1))
                .build();

        assertThatThrownBy(() -> useCase.search(invalidCpfQuery))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("CPF inválido.");
        assertThatThrownBy(() -> useCase.search(invalidStatusQuery))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("O status da reclamação é inválido.");
        assertThatThrownBy(() -> useCase.search(invalidDateRangeQuery))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessage("A data inicial deve ser menor ou igual à data final.");
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
