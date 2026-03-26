package com.complaintmanagementservice.application.usecase;

import com.complaintmanagementservice.TestFixtures;
import com.complaintmanagementservice.application.port.out.ComplaintRepositoryPort;
import com.complaintmanagementservice.application.query.SearchComplaintsQuery;
import com.complaintmanagementservice.domain.model.Complaint;
import com.complaintmanagementservice.domain.model.ComplaintStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchComplaintsUseCaseImplTest {

    @Mock
    private ComplaintRepositoryPort complaintRepositoryPort;

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
    void shouldDelegateSearchEvenWithInvalidFilterValues() {
        SearchComplaintsUseCaseImpl useCase = new SearchComplaintsUseCaseImpl(complaintRepositoryPort);
        when(complaintRepositoryPort.search(any())).thenReturn(List.of());

        SearchComplaintsQuery invalidCpfQuery = SearchComplaintsQuery.builder()
                .customerCpf("invalid")
                .build();
        SearchComplaintsQuery invalidStatusQuery = SearchComplaintsQuery.builder()
                .statusIds(List.of(99))
                .build();

        useCase.search(invalidCpfQuery);
        useCase.search(invalidStatusQuery);

        verify(complaintRepositoryPort).search(invalidCpfQuery);
        verify(complaintRepositoryPort).search(invalidStatusQuery);
    }
}
