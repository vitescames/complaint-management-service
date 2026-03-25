package com.complaintmanagementservice.domain.model;

import com.complaintmanagementservice.domain.event.ComplaintCreatedDomainEvent;
import com.complaintmanagementservice.domain.event.DomainEvent;
import com.complaintmanagementservice.domain.exception.DomainValidationException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
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

    private Complaint(Builder builder, List<DomainEvent> domainEvents) {
        ComplaintId validatedId = requireId(builder.id);
        Customer validatedCustomer = requireCustomer(builder.customer);
        LocalDate validatedComplaintDate = requireComplaintDate(builder.complaintDate);
        ComplaintText validatedComplaintText = requireComplaintText(builder.complaintText);
        ComplaintStatus validatedStatus = requireStatus(builder.status);
        Instant validatedRegisteredAt = requireRegisteredAt(builder.registeredAt);

        this.id = validatedId;
        this.customer = validatedCustomer;
        this.complaintDate = validatedComplaintDate;
        this.complaintText = validatedComplaintText;
        this.documentUrls = builder.documentUrls == null ? List.of() : List.copyOf(builder.documentUrls);
        this.status = validatedStatus;
        this.categories = builder.categories == null ? Set.of() : Set.copyOf(new LinkedHashSet<>(builder.categories));
        this.registeredAt = validatedRegisteredAt;
        this.domainEvents = new ArrayList<>(domainEvents);
    }

    public static Builder builder() {
        return new Builder();
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

    public static final class Builder {

        private ComplaintId id;
        private Customer customer;
        private LocalDate complaintDate;
        private ComplaintText complaintText;
        private List<DocumentUrl> documentUrls;
        private ComplaintStatus status;
        private Set<Category> categories;
        private Instant registeredAt;
        private Clock clock;

        private Builder() {
        }

        public Builder id(ComplaintId id) {
            this.id = id;
            return this;
        }

        public Builder customer(Customer customer) {
            this.customer = customer;
            return this;
        }

        public Builder complaintDate(LocalDate complaintDate) {
            this.complaintDate = complaintDate;
            return this;
        }

        public Builder complaintText(ComplaintText complaintText) {
            this.complaintText = complaintText;
            return this;
        }

        public Builder documentUrls(List<DocumentUrl> documentUrls) {
            this.documentUrls = documentUrls;
            return this;
        }

        public Builder status(ComplaintStatus status) {
            this.status = status;
            return this;
        }

        public Builder categories(Set<Category> categories) {
            this.categories = categories;
            return this;
        }

        public Builder registeredAt(Instant registeredAt) {
            this.registeredAt = registeredAt;
            return this;
        }

        public Builder clock(Clock clock) {
            this.clock = clock;
            return this;
        }

        public Complaint buildNew() {
            if (clock == null) {
                throw new DomainValidationException("O relógio de criação da reclamação é obrigatório.");
            }

            ComplaintId complaintId = ComplaintId.newId();
            Instant createdAt = Instant.now(clock);
            this.id = complaintId;
            this.status = ComplaintStatus.PENDING;
            this.registeredAt = createdAt;
            return new Complaint(this, List.of(new ComplaintCreatedDomainEvent(complaintId, createdAt)));
        }

        public Complaint buildReconstituted() {
            return new Complaint(this, List.of());
        }
    }

    private ComplaintId requireId(ComplaintId id) {
        if (id == null) {
            throw new DomainValidationException("O identificador da reclamação é obrigatório.");
        }
        return id;
    }

    private Customer requireCustomer(Customer customer) {
        if (customer == null) {
            throw new DomainValidationException("O cliente da reclamação é obrigatório.");
        }
        return customer;
    }

    private LocalDate requireComplaintDate(LocalDate complaintDate) {
        if (complaintDate == null) {
            throw new DomainValidationException("A data da reclamação é obrigatória.");
        }
        return complaintDate;
    }

    private ComplaintText requireComplaintText(ComplaintText complaintText) {
        if (complaintText == null) {
            throw new DomainValidationException("O texto da reclamação é obrigatório.");
        }
        return complaintText;
    }

    private ComplaintStatus requireStatus(ComplaintStatus status) {
        if (status == null) {
            throw new DomainValidationException("O status da reclamação é obrigatório.");
        }
        return status;
    }

    private Instant requireRegisteredAt(Instant registeredAt) {
        if (registeredAt == null) {
            throw new DomainValidationException("A data de registro da reclamação é obrigatória.");
        }
        return registeredAt;
    }
}
