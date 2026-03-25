package com.complaintmanagementservice.adapters.out.persistence.repository;

import com.complaintmanagementservice.adapters.out.persistence.entity.ComplaintStatusEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComplaintStatusJpaRepository extends JpaRepository<ComplaintStatusEntity, Integer> {
}
