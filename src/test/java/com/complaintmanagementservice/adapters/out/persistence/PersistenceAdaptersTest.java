package com.complaintmanagementservice.adapters.out.persistence;

import com.complaintmanagementservice.TestFixtures;
import com.complaintmanagementservice.adapters.out.persistence.entity.CategoryEntity;
import com.complaintmanagementservice.adapters.out.persistence.entity.ComplaintEntity;
import com.complaintmanagementservice.adapters.out.persistence.entity.ComplaintStatusEntity;
import com.complaintmanagementservice.adapters.out.persistence.entity.CustomerEntity;
import com.complaintmanagementservice.adapters.out.persistence.mapper.CategoryPersistenceMapper;
import com.complaintmanagementservice.adapters.out.persistence.mapper.ComplaintPersistenceMapper;
import com.complaintmanagementservice.adapters.out.persistence.repository.CategoryJpaRepository;
import com.complaintmanagementservice.adapters.out.persistence.repository.ComplaintJpaRepository;
import com.complaintmanagementservice.adapters.out.persistence.repository.ComplaintStatusJpaRepository;
import com.complaintmanagementservice.adapters.out.persistence.repository.CustomerJpaRepository;
import com.complaintmanagementservice.application.exception.InfrastructureUnavailableException;
import com.complaintmanagementservice.application.exception.PersistenceOperationException;
import com.complaintmanagementservice.application.exception.ReferenceDataNotFoundException;
import com.complaintmanagementservice.infrastructure.resilience.ResilientExecutor;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PersistenceAdaptersTest {

    @Mock
    private CategoryJpaRepository categoryJpaRepository;

    @Mock
    private ComplaintJpaRepository complaintJpaRepository;

    @Mock
    private CustomerJpaRepository customerJpaRepository;

    @Mock
    private ComplaintStatusJpaRepository complaintStatusJpaRepository;

    @Mock
    private ResilientExecutor resilientExecutor;

    @Test
    void shouldLoadCategoryCatalogThroughResilience() {
        CategoryCatalogPersistenceAdapter adapter = new CategoryCatalogPersistenceAdapter(
                categoryJpaRepository,
                new CategoryPersistenceMapper(),
                resilientExecutor
        );
        CategoryEntity entity = new CategoryEntity(4L, "acesso");
        when(categoryJpaRepository.findAllByOrderByIdAsc()).thenReturn(List.of(entity));
        doAnswer(invocation -> invocation.getArgument(1, java.util.function.Supplier.class).get())
                .when(resilientExecutor).executeSupplier(any(), any());

        assertThat(adapter.loadAll()).extracting("name").containsExactly("acesso");
    }

    @Test
    void shouldWrapCategoryCatalogFailures() {
        CategoryCatalogPersistenceAdapter adapter = new CategoryCatalogPersistenceAdapter(
                categoryJpaRepository,
                new CategoryPersistenceMapper(),
                resilientExecutor
        );
        doThrow(CallNotPermittedException.createCallNotPermittedException(
                io.github.resilience4j.circuitbreaker.CircuitBreaker.ofDefaults("catalog")
        )).when(resilientExecutor).executeSupplier(any(), any());

        assertThatThrownBy(adapter::loadAll)
                .isInstanceOf(InfrastructureUnavailableException.class);

        org.mockito.Mockito.reset(resilientExecutor);
        doThrow(new IllegalStateException("catalog failure")).when(resilientExecutor).executeSupplier(any(), any());

        assertThatThrownBy(adapter::loadAll)
                .isInstanceOf(PersistenceOperationException.class);
    }

    @Test
    void shouldPersistAndSearchComplaints() {
        ComplaintPersistenceMapper mapper = new ComplaintPersistenceMapper(new CategoryPersistenceMapper());
        ComplaintPersistenceAdapter adapter = new ComplaintPersistenceAdapter(
                complaintJpaRepository,
                customerJpaRepository,
                complaintStatusJpaRepository,
                categoryJpaRepository,
                mapper,
                resilientExecutor
        );
        ComplaintStatusEntity statusEntity = new ComplaintStatusEntity(1, "PENDING");
        CustomerEntity customerEntity = new CustomerEntity("52998224725", "Maria Silva", LocalDate.of(1990, 6, 15), "maria.silva@example.com");
        CategoryEntity categoryEntity = new CategoryEntity(4L, "acesso");
        ComplaintEntity savedEntity = mapper.toEntity(TestFixtures.complaint(), customerEntity, statusEntity, Set.of(categoryEntity));
        when(customerJpaRepository.save(any())).thenReturn(customerEntity);
        when(complaintStatusJpaRepository.findById(1)).thenReturn(Optional.of(statusEntity));
        when(categoryJpaRepository.findAllById(any())).thenReturn(List.of(categoryEntity, new CategoryEntity(3L, "cobranca")));
        when(complaintJpaRepository.save(any())).thenReturn(savedEntity);
        when(complaintJpaRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Sort.class)))
                .thenReturn(List.of(savedEntity));
        when(complaintJpaRepository.findByComplaintDateAndStatusIdNotOrderByComplaintDateDesc(LocalDate.of(2026, 3, 16), 3))
                .thenReturn(List.of(savedEntity));
        doAnswer(invocation -> invocation.getArgument(1, java.util.function.Supplier.class).get())
                .when(resilientExecutor).executeSupplier(any(), any());

        assertThat(adapter.save(TestFixtures.complaint()).id()).isEqualTo(TestFixtures.complaint().id());
        assertThat(adapter.search(TestFixtures.searchQuery())).hasSize(1);
        assertThat(adapter.findNonResolvedComplaintsCreatedOn(LocalDate.of(2026, 3, 16))).hasSize(1);
    }

    @Test
    void shouldFailWhenStatusReferenceIsMissing() {
        ComplaintPersistenceAdapter adapter = new ComplaintPersistenceAdapter(
                complaintJpaRepository,
                customerJpaRepository,
                complaintStatusJpaRepository,
                categoryJpaRepository,
                new ComplaintPersistenceMapper(new CategoryPersistenceMapper()),
                resilientExecutor
        );
        when(customerJpaRepository.save(any())).thenReturn(new CustomerEntity("52998224725", "Maria", LocalDate.of(1990, 6, 15), "maria@example.com"));
        when(complaintStatusJpaRepository.findById(1)).thenReturn(Optional.empty());
        doAnswer(invocation -> invocation.getArgument(1, java.util.function.Supplier.class).get())
                .when(resilientExecutor).executeSupplier(any(), any());

        assertThatThrownBy(() -> adapter.save(TestFixtures.complaint()))
                .isInstanceOf(ReferenceDataNotFoundException.class)
                .hasMessage("O status de referencia da reclamacao nao foi encontrado");
    }

    @Test
    void shouldWrapCircuitBreakerFailures() {
        ComplaintPersistenceAdapter adapter = new ComplaintPersistenceAdapter(
                complaintJpaRepository,
                customerJpaRepository,
                complaintStatusJpaRepository,
                categoryJpaRepository,
                new ComplaintPersistenceMapper(new CategoryPersistenceMapper()),
                resilientExecutor
        );
        doThrow(CallNotPermittedException.createCallNotPermittedException(
                io.github.resilience4j.circuitbreaker.CircuitBreaker.ofDefaults("persistence")
        )).when(resilientExecutor).executeSupplier(any(), any());

        assertThatThrownBy(() -> adapter.search(TestFixtures.searchQuery()))
                .isInstanceOf(InfrastructureUnavailableException.class);
    }

    @Test
    void shouldWrapGenericPersistenceFailures() {
        ComplaintPersistenceAdapter adapter = new ComplaintPersistenceAdapter(
                complaintJpaRepository,
                customerJpaRepository,
                complaintStatusJpaRepository,
                categoryJpaRepository,
                new ComplaintPersistenceMapper(new CategoryPersistenceMapper()),
                resilientExecutor
        );
        doThrow(new IllegalStateException("boom")).when(resilientExecutor).executeSupplier(any(), any());

        assertThatThrownBy(() -> adapter.findNonResolvedComplaintsCreatedOn(LocalDate.now()))
                .isInstanceOf(PersistenceOperationException.class);
    }

    @Test
    void shouldWrapAdditionalPersistenceFailureScenarios() {
        ComplaintPersistenceAdapter adapter = new ComplaintPersistenceAdapter(
                complaintJpaRepository,
                customerJpaRepository,
                complaintStatusJpaRepository,
                categoryJpaRepository,
                new ComplaintPersistenceMapper(new CategoryPersistenceMapper()),
                resilientExecutor
        );

        doThrow(new IllegalStateException("save failed")).when(resilientExecutor).executeSupplier(any(), any());
        assertThatThrownBy(() -> adapter.save(TestFixtures.complaint()))
                .isInstanceOf(PersistenceOperationException.class);

        org.mockito.Mockito.reset(resilientExecutor);
        doThrow(CallNotPermittedException.createCallNotPermittedException(
                io.github.resilience4j.circuitbreaker.CircuitBreaker.ofDefaults("save-unavailable")
        )).when(resilientExecutor).executeSupplier(any(), any());
        assertThatThrownBy(() -> adapter.save(TestFixtures.complaint()))
                .isInstanceOf(InfrastructureUnavailableException.class);

        org.mockito.Mockito.reset(resilientExecutor);
        doThrow(new IllegalStateException("search failed")).when(resilientExecutor).executeSupplier(any(), any());
        assertThatThrownBy(() -> adapter.search(TestFixtures.searchQuery()))
                .isInstanceOf(PersistenceOperationException.class);

        org.mockito.Mockito.reset(resilientExecutor);
        doThrow(CallNotPermittedException.createCallNotPermittedException(
                io.github.resilience4j.circuitbreaker.CircuitBreaker.ofDefaults("find-non-resolved")
        )).when(resilientExecutor).executeSupplier(any(), any());
        assertThatThrownBy(() -> adapter.findNonResolvedComplaintsCreatedOn(LocalDate.now()))
                .isInstanceOf(InfrastructureUnavailableException.class);
    }
}
