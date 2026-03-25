package com.complaintmanagementservice.domain.model;

import com.complaintmanagementservice.domain.event.ComplaintCreatedDomainEvent;
import com.complaintmanagementservice.domain.event.DomainEvent;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class Complaint {

    private final ComplaintId id;
    private final Customer customer;
    private final LocalDate complaintDate;
    private final ComplaintText complaintText;
    private final List<DocumentUrl> documentUrls;
    private final ComplaintStatus status;
    private final Set<Category> categories;
    private final Instant registeredAt;
    private final List<DomainEvent> domainEvents;

    private Complaint(
            ComplaintId id,
            Customer customer,
            LocalDate complaintDate,
            ComplaintText complaintText,
            List<DocumentUrl> documentUrls,
            ComplaintStatus status,
            Set<Category> categories,
            Instant registeredAt,
            List<DomainEvent> domainEvents
    ) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.customer = Objects.requireNonNull(customer, "customer must not be null");
        this.complaintDate = Objects.requireNonNull(complaintDate, "complaintDate must not be null");
        this.complaintText = Objects.requireNonNull(complaintText, "complaintText must not be null");
        this.documentUrls = List.copyOf(Objects.requireNonNull(documentUrls, "documentUrls must not be null"));
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.categories = Set.copyOf(new LinkedHashSet<>(Objects.requireNonNull(categories, "categories must not be null")));
        this.registeredAt = Objects.requireNonNull(registeredAt, "registeredAt must not be null");
        this.domainEvents = new ArrayList<>(Objects.requireNonNull(domainEvents, "domainEvents must not be null"));
    }

    public static Complaint create(
            Customer customer,
            LocalDate complaintDate,
            ComplaintText complaintText,
            List<DocumentUrl> documentUrls,
            Set<Category> categories,
            Clock clock
    ) {
        Instant now = Instant.now(Objects.requireNonNull(clock, "clock must not be null"));
        ComplaintId complaintId = ComplaintId.newId();
        return new Complaint(
                complaintId,
                customer,
                complaintDate,
                complaintText,
                documentUrls,
                ComplaintStatus.PENDING,
                categories,
                now,
                List.of(new ComplaintCreatedDomainEvent(complaintId, now))
        );
    }

    public static Complaint restore(
            ComplaintId id,
            Customer customer,
            LocalDate complaintDate,
            ComplaintText complaintText,
            List<DocumentUrl> documentUrls,
            ComplaintStatus status,
            Set<Category> categories,
            Instant registeredAt
    ) {
        return new Complaint(id, customer, complaintDate, complaintText, documentUrls, status, categories, registeredAt, List.of());
    }

    public ComplaintId id() {
        return id;
    }

    public Customer customer() {
        return customer;
    }

    public LocalDate complaintDate() {
        return complaintDate;
    }

    public ComplaintText complaintText() {
        return complaintText;
    }

    public List<DocumentUrl> documentUrls() {
        return documentUrls;
    }

    public ComplaintStatus status() {
        return status;
    }

    public Set<Category> categories() {
        return categories;
    }

    public Instant registeredAt() {
        return registeredAt;
    }

    public List<DomainEvent> pullDomainEvents() {
        List<DomainEvent> pulledEvents = List.copyOf(domainEvents);
        domainEvents.clear();
        return pulledEvents;
    }
}
