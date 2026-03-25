package com.complaintmanagementservice.adapters.out.persistence.repository;

import com.complaintmanagementservice.adapters.out.persistence.entity.ComplaintEntity;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.NonNull;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ComplaintJpaRepository extends JpaRepository<ComplaintEntity, UUID>, JpaSpecificationExecutor<ComplaintEntity> {

    @Override
    @EntityGraph(attributePaths = {"customer", "status", "categories", "documents"})
    List<ComplaintEntity> findAll(@NonNull Specification<ComplaintEntity> specification, @NonNull Sort sort);

    @EntityGraph(attributePaths = {"customer", "status", "categories", "documents"})
    List<ComplaintEntity> findByComplaintDateAndStatusIdNotOrderByComplaintDateDesc(@NonNull LocalDate complaintDate, @NonNull Integer statusId);
}
