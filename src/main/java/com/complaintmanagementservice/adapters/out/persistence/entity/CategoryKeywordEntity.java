package com.complaintmanagementservice.adapters.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "category_keywords")
public class CategoryKeywordEntity {

    @Id
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private CategoryEntity category;

    @Column(name = "keyword_value", nullable = false, length = 120)
    private String value;

    protected CategoryKeywordEntity() {
    }

    public CategoryKeywordEntity(Long id, CategoryEntity category, String value) {
        this.id = id;
        this.category = category;
        this.value = value;
    }

    public Long getId() {
        return id;
    }

    public CategoryEntity getCategory() {
        return category;
    }

    public String getValue() {
        return value;
    }
}
