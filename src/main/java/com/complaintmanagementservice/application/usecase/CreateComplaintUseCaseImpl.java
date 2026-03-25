package com.complaintmanagementservice.application.usecase;

import com.complaintmanagementservice.application.command.CreateComplaintCommand;
import com.complaintmanagementservice.application.event.DomainEventPublisher;
import com.complaintmanagementservice.application.exception.ReferenceDataNotFoundException;
import com.complaintmanagementservice.application.port.in.CreateComplaintUseCase;
import com.complaintmanagementservice.application.port.out.CategoryCatalogPort;
import com.complaintmanagementservice.application.port.out.ComplaintRepositoryPort;
import com.complaintmanagementservice.domain.event.DomainEvent;
import com.complaintmanagementservice.domain.model.Category;
import com.complaintmanagementservice.domain.model.Complaint;
import com.complaintmanagementservice.domain.model.ComplaintText;
import com.complaintmanagementservice.domain.model.Cpf;
import com.complaintmanagementservice.domain.model.Customer;
import com.complaintmanagementservice.domain.model.CustomerName;
import com.complaintmanagementservice.domain.model.DocumentUrl;
import com.complaintmanagementservice.domain.model.EmailAddress;
import com.complaintmanagementservice.domain.service.ComplaintCategoryClassifier;

import java.time.Clock;
import java.util.List;
import java.util.Set;

public class CreateComplaintUseCaseImpl implements CreateComplaintUseCase {

    private final CategoryCatalogPort categoryCatalogPort;
    private final ComplaintRepositoryPort complaintRepositoryPort;
    private final DomainEventPublisher domainEventPublisher;
    private final ComplaintCategoryClassifier complaintCategoryClassifier;
    private final Clock clock;

    public CreateComplaintUseCaseImpl(
            CategoryCatalogPort categoryCatalogPort,
            ComplaintRepositoryPort complaintRepositoryPort,
            DomainEventPublisher domainEventPublisher,
            ComplaintCategoryClassifier complaintCategoryClassifier,
            Clock clock
    ) {
        this.categoryCatalogPort = categoryCatalogPort;
        this.complaintRepositoryPort = complaintRepositoryPort;
        this.domainEventPublisher = domainEventPublisher;
        this.complaintCategoryClassifier = complaintCategoryClassifier;
        this.clock = clock;
    }

    @Override
    public Complaint create(CreateComplaintCommand command) {
        List<Category> categories = categoryCatalogPort.loadAll();
        if (categories.isEmpty()) {
            throw new ReferenceDataNotFoundException("O catalogo de categorias de reclamacao nao esta configurado");
        }

        ComplaintText complaintText = new ComplaintText(command.complaintText());
        Set<Category> matchedCategories = complaintCategoryClassifier.classify(complaintText, categories);

        Complaint complaint = Complaint.builder()
                .customer(Customer.builder()
                        .cpf(new Cpf(command.customerCpf()))
                        .name(new CustomerName(command.customerName()))
                        .birthDate(command.customerBirthDate())
                        .emailAddress(new EmailAddress(command.customerEmail()))
                        .build())
                .complaintDate(command.complaintCreatedDate())
                .complaintText(complaintText)
                .documentUrls(command.documentUrls().stream().map(DocumentUrl::new).toList())
                .categories(matchedCategories)
                .clock(clock)
                .buildNew();

        List<DomainEvent> domainEvents = complaint.pullDomainEvents();
        Complaint savedComplaint = complaintRepositoryPort.save(complaint);
        domainEvents.forEach(domainEventPublisher::publish);
        return savedComplaint;
    }
}
