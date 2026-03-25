package com.complaintmanagementservice.adapters.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "complaint_documents")
public class ComplaintDocumentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "complaint_id", nullable = false)
    private ComplaintEntity complaint;

    @Column(name = "document_url", nullable = false, length = 1000)
    private String documentUrl;

    protected ComplaintDocumentEntity() {
    }

    public ComplaintDocumentEntity(String documentUrl) {
        this.documentUrl = documentUrl;
    }

    public Long getId() {
        return id;
    }

    public ComplaintEntity getComplaint() {
        return complaint;
    }

    public void setComplaint(ComplaintEntity complaint) {
        this.complaint = complaint;
    }

    public String getDocumentUrl() {
        return documentUrl;
    }
}
