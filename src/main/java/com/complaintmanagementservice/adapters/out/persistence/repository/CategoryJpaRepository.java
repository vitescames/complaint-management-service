package com.complaintmanagementservice.adapters.out.persistence.repository;

import com.complaintmanagementservice.adapters.out.persistence.entity.CategoryEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryJpaRepository extends JpaRepository<CategoryEntity, Long> {

    @EntityGraph(attributePaths = "keywords")
    List<CategoryEntity> findAllByOrderByIdAsc();
}
