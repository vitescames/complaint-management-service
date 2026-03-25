package com.complaintmanagementservice.adapters.out.persistence.repository;

import com.complaintmanagementservice.adapters.out.persistence.entity.CustomerEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerJpaRepository extends JpaRepository<CustomerEntity, String> {
}
