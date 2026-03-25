package com.complaintmanagementservice.adapters.out.persistence.mapper;

import com.complaintmanagementservice.adapters.out.persistence.entity.CategoryEntity;
import com.complaintmanagementservice.domain.model.Category;
import com.complaintmanagementservice.domain.model.CategoryKeyword;

import java.util.Set;

public class CategoryPersistenceMapper {

    public Category toDomain(CategoryEntity entity) {
        Set<CategoryKeyword> keywords = entity.getKeywords().stream()
                .map(keywordEntity -> new CategoryKeyword(keywordEntity.getId(), keywordEntity.getValue()))
                .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
        return new Category(entity.getId(), entity.getName(), keywords);
    }

    public Category toComplaintCategory(CategoryEntity entity) {
        return new Category(entity.getId(), entity.getName(), Set.of());
    }
}
