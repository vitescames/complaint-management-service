package com.complaintmanagementservice.application.usecase;

import com.complaintmanagementservice.application.command.CreateComplaintCommand;
import com.complaintmanagementservice.application.exception.ReferenceDataNotFoundException;
import com.complaintmanagementservice.application.port.in.CreateComplaintUseCase;
import com.complaintmanagementservice.application.port.out.CategoryCatalogPort;
import com.complaintmanagementservice.application.port.out.ComplaintRepositoryPort;
import com.complaintmanagementservice.application.port.out.DomainEventPublisherPort;
import com.complaintmanagementservice.domain.event.DomainEvent;
import com.complaintmanagementservice.domain.model.Category;
import com.complaintmanagementservice.domain.model.Complaint;
import com.complaintmanagementservice.domain.model.Customer;
import com.complaintmanagementservice.domain.service.ComplaintCategoryClassifier;

import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class CreateComplaintService implements CreateComplaintUseCase {

    private final CategoryCatalogPort categoryCatalogPort;
    private final ComplaintRepositoryPort complaintRepositoryPort;
    private final DomainEventPublisherPort domainEventPublisherPort;
    private final ComplaintCategoryClassifier complaintCategoryClassifier;
    private final Clock clock;

    public CreateComplaintService(
            CategoryCatalogPort categoryCatalogPort,
            ComplaintRepositoryPort complaintRepositoryPort,
            DomainEventPublisherPort domainEventPublisherPort,
            ComplaintCategoryClassifier complaintCategoryClassifier,
            Clock clock
    ) {
        this.categoryCatalogPort = Objects.requireNonNull(categoryCatalogPort, "categoryCatalogPort must not be null");
        this.complaintRepositoryPort = Objects.requireNonNull(complaintRepositoryPort, "complaintRepositoryPort must not be null");
        this.domainEventPublisherPort = Objects.requireNonNull(domainEventPublisherPort, "domainEventPublisherPort must not be null");
        this.complaintCategoryClassifier = Objects.requireNonNull(complaintCategoryClassifier, "complaintCategoryClassifier must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public Complaint create(CreateComplaintCommand command) {
        List<Category> categories = categoryCatalogPort.loadAll();
        if (categories.isEmpty()) {
            throw new ReferenceDataNotFoundException("Complaint category catalog must be configured");
        }

        Customer customer = new Customer(
                command.customerCpf(),
                command.customerName(),
                command.customerBirthDate(),
                command.customerEmail()
        );

        Set<Category> matchedCategories = complaintCategoryClassifier.classify(command.complaintText(), categories);
        Complaint complaint = Complaint.create(
                customer,
                command.complaintCreatedDate(),
                command.complaintText(),
                command.documentUrls(),
                matchedCategories,
                clock
        );

        List<DomainEvent> domainEvents = complaint.pullDomainEvents();
        Complaint savedComplaint = complaintRepositoryPort.save(complaint);
        domainEvents.forEach(domainEventPublisherPort::publish);
        return savedComplaint;
    }
}
