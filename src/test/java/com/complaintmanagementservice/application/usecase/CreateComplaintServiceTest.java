package com.complaintmanagementservice.application.usecase;

import com.complaintmanagementservice.TestFixtures;
import com.complaintmanagementservice.application.exception.ReferenceDataNotFoundException;
import com.complaintmanagementservice.application.port.out.CategoryCatalogPort;
import com.complaintmanagementservice.application.port.out.ComplaintRepositoryPort;
import com.complaintmanagementservice.application.port.out.DomainEventPublisherPort;
import com.complaintmanagementservice.domain.event.DomainEvent;
import com.complaintmanagementservice.domain.model.Complaint;
import com.complaintmanagementservice.domain.service.ComplaintCategoryClassifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateComplaintServiceTest {

    @Mock
    private CategoryCatalogPort categoryCatalogPort;

    @Mock
    private ComplaintRepositoryPort complaintRepositoryPort;

    @Mock
    private DomainEventPublisherPort domainEventPublisherPort;

    @Test
    void shouldCreateComplaintAndPublishDomainEvent() {
        CreateComplaintService service = new CreateComplaintService(
                categoryCatalogPort,
                complaintRepositoryPort,
                domainEventPublisherPort,
                new ComplaintCategoryClassifier(),
                TestFixtures.FIXED_CLOCK
        );
        Complaint savedComplaint = TestFixtures.createdComplaint();
        when(categoryCatalogPort.loadAll()).thenReturn(List.of(TestFixtures.acessoCategory(), TestFixtures.cobrancaCategory()));
        when(complaintRepositoryPort.save(any(Complaint.class))).thenReturn(savedComplaint);

        Complaint result = service.create(TestFixtures.createComplaintCommand());

        assertThat(result).isEqualTo(savedComplaint);
        ArgumentCaptor<Complaint> complaintCaptor = ArgumentCaptor.forClass(Complaint.class);
        verify(complaintRepositoryPort).save(complaintCaptor.capture());
        assertThat(complaintCaptor.getValue().status().name()).isEqualTo("PENDING");
        verify(domainEventPublisherPort).publish(any(DomainEvent.class));
    }

    @Test
    void shouldRejectCreationWhenCatalogIsMissing() {
        CreateComplaintService service = new CreateComplaintService(
                categoryCatalogPort,
                complaintRepositoryPort,
                domainEventPublisherPort,
                new ComplaintCategoryClassifier(),
                TestFixtures.FIXED_CLOCK
        );
        when(categoryCatalogPort.loadAll()).thenReturn(List.of());

        assertThatThrownBy(() -> service.create(TestFixtures.createComplaintCommand()))
                .isInstanceOf(ReferenceDataNotFoundException.class)
                .hasMessage("Complaint category catalog must be configured");

        verify(complaintRepositoryPort, never()).save(any());
    }

    @Test
    void shouldNotPublishEventWhenPersistenceFails() {
        CreateComplaintService service = new CreateComplaintService(
                categoryCatalogPort,
                complaintRepositoryPort,
                domainEventPublisherPort,
                new ComplaintCategoryClassifier(),
                TestFixtures.FIXED_CLOCK
        );
        when(categoryCatalogPort.loadAll()).thenReturn(List.of(TestFixtures.acessoCategory()));
        doThrow(new IllegalStateException("database unavailable")).when(complaintRepositoryPort).save(any(Complaint.class));

        assertThatThrownBy(() -> service.create(TestFixtures.createComplaintCommand()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("database unavailable");

        verify(domainEventPublisherPort, never()).publish(any());
    }
}
