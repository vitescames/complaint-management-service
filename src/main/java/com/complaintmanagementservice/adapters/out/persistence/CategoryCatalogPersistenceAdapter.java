package com.complaintmanagementservice.adapters.out.persistence;

import com.complaintmanagementservice.adapters.out.persistence.mapper.CategoryPersistenceMapper;
import com.complaintmanagementservice.adapters.out.persistence.repository.CategoryJpaRepository;
import com.complaintmanagementservice.application.port.out.CategoryCatalogPort;
import com.complaintmanagementservice.domain.model.Category;
import com.complaintmanagementservice.infrastructure.resilience.ResilienceProfile;
import com.complaintmanagementservice.infrastructure.resilience.ResilientExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class CategoryCatalogPersistenceAdapter implements CategoryCatalogPort {

    private final CategoryJpaRepository categoryJpaRepository;
    private final CategoryPersistenceMapper categoryPersistenceMapper;
    private final ResilientExecutor resilientExecutor;

    public CategoryCatalogPersistenceAdapter(
            CategoryJpaRepository categoryJpaRepository,
            CategoryPersistenceMapper categoryPersistenceMapper,
            ResilientExecutor resilientExecutor
    ) {
        this.categoryJpaRepository = categoryJpaRepository;
        this.categoryPersistenceMapper = categoryPersistenceMapper;
        this.resilientExecutor = resilientExecutor;
    }

    @Override
    public List<Category> loadAll() {
        return resilientExecutor.executeSupplier(
                ResilienceProfile.PERSISTENCE,
                () -> categoryJpaRepository.findAllByOrderByIdAsc().stream()
                        .map(categoryPersistenceMapper::toDomain)
                        .toList()
        );
    }
}
