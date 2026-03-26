package com.complaintmanagementservice.application.usecase;

import com.complaintmanagementservice.TestFixtures;
import com.complaintmanagementservice.application.command.CreateComplaintCommand;
import com.complaintmanagementservice.application.event.DomainEventPublisher;
import com.complaintmanagementservice.application.exception.BusinessRuleViolationException;
import com.complaintmanagementservice.application.exception.ReferenceDataNotFoundException;
import com.complaintmanagementservice.application.port.out.CategoryCatalogPort;
import com.complaintmanagementservice.application.port.out.ComplaintRepositoryPort;
import com.complaintmanagementservice.domain.event.DomainEvent;
import com.complaintmanagementservice.domain.exception.DomainValidationException;
import com.complaintmanagementservice.domain.model.Complaint;
import com.complaintmanagementservice.domain.service.ComplaintCategoryClassifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateComplaintUseCaseImplTest {

    @Mock
    private CategoryCatalogPort categoryCatalogPort;

    @Mock
    private ComplaintRepositoryPort complaintRepositoryPort;

    @Mock
    private DomainEventPublisher domainEventPublisher;

    @Test
    void shouldCreateComplaintAndPublishDomainEvent() {
        CreateComplaintUseCaseImpl useCase = new CreateComplaintUseCaseImpl(
                categoryCatalogPort,
                complaintRepositoryPort,
                domainEventPublisher,
                new ComplaintCategoryClassifier(),
                TestFixtures.FIXED_CLOCK
        );
        Complaint savedComplaint = TestFixtures.createdComplaint();
        when(categoryCatalogPort.loadAll()).thenReturn(List.of(TestFixtures.acessoCategory(), TestFixtures.cobrancaCategory()));
        when(complaintRepositoryPort.save(any(Complaint.class))).thenReturn(savedComplaint);

        Complaint result = useCase.create(TestFixtures.createComplaintCommand());

        assertThat(result).isEqualTo(savedComplaint);
        ArgumentCaptor<Complaint> complaintCaptor = ArgumentCaptor.forClass(Complaint.class);
        verify(complaintRepositoryPort).save(complaintCaptor.capture());
        assertThat(complaintCaptor.getValue().status().name()).isEqualTo("PENDING");
        verify(domainEventPublisher).publish(any(DomainEvent.class));
    }

    @Test
    void shouldRejectCreationWhenCatalogIsMissing() {
        CreateComplaintUseCaseImpl useCase = new CreateComplaintUseCaseImpl(
                categoryCatalogPort,
                complaintRepositoryPort,
                domainEventPublisher,
                new ComplaintCategoryClassifier(),
                TestFixtures.FIXED_CLOCK
        );
        when(categoryCatalogPort.loadAll()).thenReturn(List.of());
        CreateComplaintCommand command = TestFixtures.createComplaintCommand();

        assertThatThrownBy(() -> useCase.create(command))
                .isInstanceOf(ReferenceDataNotFoundException.class)
                .hasMessage("O catálogo de categorias de reclamação não está configurado.");

        verify(complaintRepositoryPort, never()).save(any());
    }

    @Test
    void shouldRejectFutureComplaintDate() {
        CreateComplaintUseCaseImpl useCase = new CreateComplaintUseCaseImpl(
                categoryCatalogPort,
                complaintRepositoryPort,
                domainEventPublisher,
                new ComplaintCategoryClassifier(),
                TestFixtures.FIXED_CLOCK
        );
        when(categoryCatalogPort.loadAll()).thenReturn(List.of(TestFixtures.acessoCategory()));
        CreateComplaintCommand command = CreateComplaintCommand.builder()
                .customerCpf("52998224725")
                .customerName("Maria Silva")
                .customerBirthDate(LocalDate.of(1990, 6, 15))
                .customerEmail("maria.silva@example.com")
                .complaintCreatedDate(LocalDate.of(2026, 3, 24))
                .complaintText("Meu login falha")
                .build();

        assertThatThrownBy(() -> useCase.create(command))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessage("A data da reclamação não pode ser futura.");
    }

    @Test
    void shouldDelegateInvalidDocumentUrlValidationToDomain() {
        CreateComplaintUseCaseImpl useCase = new CreateComplaintUseCaseImpl(
                categoryCatalogPort,
                complaintRepositoryPort,
                domainEventPublisher,
                new ComplaintCategoryClassifier(),
                TestFixtures.FIXED_CLOCK
        );
        when(categoryCatalogPort.loadAll()).thenReturn(List.of(TestFixtures.acessoCategory()));
        CreateComplaintCommand command = CreateComplaintCommand.builder()
                .customerCpf("52998224725")
                .customerName("Maria Silva")
                .customerBirthDate(LocalDate.of(1990, 6, 15))
                .customerEmail("maria.silva@example.com")
                .complaintCreatedDate(LocalDate.of(2026, 3, 20))
                .complaintText("Meu login falha")
                .documentUrls(List.of("ftp://example.com/documento"))
                .build();

        assertThatThrownBy(() -> useCase.create(command))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("A URL do documento deve ser HTTP ou HTTPS absoluta.");
    }

    @Test
    void shouldNotPublishEventWhenPersistenceFails() {
        CreateComplaintUseCaseImpl useCase = new CreateComplaintUseCaseImpl(
                categoryCatalogPort,
                complaintRepositoryPort,
                domainEventPublisher,
                new ComplaintCategoryClassifier(),
                TestFixtures.FIXED_CLOCK
        );
        when(categoryCatalogPort.loadAll()).thenReturn(List.of(TestFixtures.acessoCategory()));
        doThrow(new IllegalStateException("database unavailable")).when(complaintRepositoryPort).save(any(Complaint.class));
        CreateComplaintCommand command = TestFixtures.createComplaintCommand();

        assertThatThrownBy(() -> useCase.create(command))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("database unavailable");

        verify(domainEventPublisher, never()).publish(any());
    }
}
