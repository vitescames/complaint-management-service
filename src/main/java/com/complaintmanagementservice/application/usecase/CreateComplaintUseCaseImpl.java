package com.complaintmanagementservice.application.usecase;

import com.complaintmanagementservice.application.command.CreateComplaintCommand;
import com.complaintmanagementservice.application.event.DomainEventPublisher;
import com.complaintmanagementservice.application.exception.BusinessRuleViolationException;
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
import java.time.LocalDate;
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
        List<Category> categories = loadCategories();
        ComplaintText complaintText = new ComplaintText(command.complaintText());

        Complaint complaint = Complaint.builder()
                .customer(buildCustomer(command))
                .complaintDate(validateComplaintCreatedDate(command.complaintCreatedDate()))
                .complaintText(complaintText)
                .documentUrls(command.documentUrls().stream().map(DocumentUrl::new).toList())
                .categories(classifyCategories(complaintText, categories))
                .clock(clock)
                .buildNew();

        List<DomainEvent> domainEvents = complaint.pullDomainEvents();
        Complaint savedComplaint = complaintRepositoryPort.save(complaint);
        domainEvents.forEach(domainEventPublisher::publish);
        return savedComplaint;
    }

    private List<Category> loadCategories() {
        List<Category> categories = categoryCatalogPort.loadAll();
        if (categories.isEmpty()) {
            throw new ReferenceDataNotFoundException("O catálogo de categorias de reclamação não está configurado.");
        }
        return categories;
    }

    private Customer buildCustomer(CreateComplaintCommand command) {
        return Customer.builder()
                .cpf(new Cpf(command.customerCpf()))
                .name(new CustomerName(command.customerName()))
                .birthDate(command.customerBirthDate())
                .emailAddress(new EmailAddress(command.customerEmail()))
                .build();
    }

    private Set<Category> classifyCategories(ComplaintText complaintText, List<Category> categories) {
        return complaintCategoryClassifier.classify(complaintText, categories);
    }

    private LocalDate validateComplaintCreatedDate(LocalDate complaintCreatedDate) {
        if (complaintCreatedDate != null && complaintCreatedDate.isAfter(LocalDate.now(clock))) {
            throw new BusinessRuleViolationException("A data da reclamação não pode ser futura.");
        }
        return complaintCreatedDate;
    }
}
