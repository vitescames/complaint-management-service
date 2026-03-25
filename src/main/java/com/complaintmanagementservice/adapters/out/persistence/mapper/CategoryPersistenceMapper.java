package com.complaintmanagementservice.adapters.out.persistence.mapper;

import com.complaintmanagementservice.adapters.out.persistence.entity.CategoryEntity;
import com.complaintmanagementservice.domain.model.Category;
import com.complaintmanagementservice.domain.model.CategoryKeyword;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class CategoryPersistenceMapper {

    public Category toDomain(CategoryEntity entity) {
        Set<CategoryKeyword> keywords = entity.getKeywords().stream()
                .map(keywordEntity -> new CategoryKeyword(keywordEntity.getId(), keywordEntity.getValue()))
                .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
        return Category.builder()
                .id(entity.getId())
                .name(entity.getName())
                .keywords(keywords)
                .build();
    }

    public Category toComplaintCategory(CategoryEntity entity) {
        return Category.builder()
                .id(entity.getId())
                .name(entity.getName())
                .keywords(Set.of())
                .build();
    }
}
