package com.complaintmanagementservice.adapters.out.persistence.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "complaints")
public class ComplaintEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_cpf", nullable = false)
    private CustomerEntity customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_id", nullable = false)
    private ComplaintStatusEntity status;

    @Column(name = "complaint_date", nullable = false)
    private LocalDate complaintDate;

    @Column(name = "complaint_text", nullable = false, length = 4000)
    private String complaintText;

    @Column(name = "registered_at", nullable = false)
    private Instant registeredAt;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "complaint_categories",
            joinColumns = @JoinColumn(name = "complaint_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    private Set<CategoryEntity> categories = new LinkedHashSet<>();

    @OneToMany(mappedBy = "complaint", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ComplaintDocumentEntity> documents = new ArrayList<>();

    protected ComplaintEntity() {
    }

    public ComplaintEntity(
            UUID id,
            CustomerEntity customer,
            ComplaintStatusEntity status,
            LocalDate complaintDate,
            String complaintText,
            Instant registeredAt,
            Set<CategoryEntity> categories
    ) {
        this.id = id;
        this.customer = customer;
        this.status = status;
        this.complaintDate = complaintDate;
        this.complaintText = complaintText;
        this.registeredAt = registeredAt;
        this.categories = categories;
    }

    public UUID getId() {
        return id;
    }

    public CustomerEntity getCustomer() {
        return customer;
    }

    public ComplaintStatusEntity getStatus() {
        return status;
    }

    public LocalDate getComplaintDate() {
        return complaintDate;
    }

    public String getComplaintText() {
        return complaintText;
    }

    public Instant getRegisteredAt() {
        return registeredAt;
    }

    public Set<CategoryEntity> getCategories() {
        return categories;
    }

    public List<ComplaintDocumentEntity> getDocuments() {
        return documents;
    }

    public void replaceDocuments(List<ComplaintDocumentEntity> newDocuments) {
        documents.clear();
        newDocuments.forEach(document -> {
            document.setComplaint(this);
            documents.add(document);
        });
    }
}
