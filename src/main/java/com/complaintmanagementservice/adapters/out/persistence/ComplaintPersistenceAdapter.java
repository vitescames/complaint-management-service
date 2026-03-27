package com.complaintmanagementservice.adapters.out.persistence;

import com.complaintmanagementservice.adapters.out.persistence.entity.CategoryEntity;
import com.complaintmanagementservice.adapters.out.persistence.entity.ComplaintEntity;
import com.complaintmanagementservice.adapters.out.persistence.entity.ComplaintStatusEntity;
import com.complaintmanagementservice.adapters.out.persistence.entity.CustomerEntity;
import com.complaintmanagementservice.adapters.out.persistence.mapper.ComplaintPersistenceMapper;
import com.complaintmanagementservice.adapters.out.persistence.repository.CategoryJpaRepository;
import com.complaintmanagementservice.adapters.out.persistence.repository.ComplaintJpaRepository;
import com.complaintmanagementservice.adapters.out.persistence.repository.ComplaintStatusJpaRepository;
import com.complaintmanagementservice.adapters.out.persistence.repository.CustomerJpaRepository;
import com.complaintmanagementservice.application.exception.ReferenceDataNotFoundException;
import com.complaintmanagementservice.application.port.out.ComplaintRepositoryPort;
import com.complaintmanagementservice.application.query.SearchComplaintsQuery;
import com.complaintmanagementservice.domain.model.Category;
import com.complaintmanagementservice.domain.model.Complaint;
import com.complaintmanagementservice.domain.model.ComplaintStatus;
import com.complaintmanagementservice.adapters.out.resilience.ResilienceProfile;
import com.complaintmanagementservice.adapters.out.resilience.ResilientExecutor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
public class ComplaintPersistenceAdapter implements ComplaintRepositoryPort {

    private static final Sort COMPLAINT_DATE_DESC = Sort.by(Sort.Direction.DESC, "complaintDate");

    private final ComplaintJpaRepository complaintJpaRepository;
    private final CustomerJpaRepository customerJpaRepository;
    private final ComplaintStatusJpaRepository complaintStatusJpaRepository;
    private final CategoryJpaRepository categoryJpaRepository;
    private final ComplaintPersistenceMapper complaintPersistenceMapper;
    private final ResilientExecutor resilientExecutor;

    public ComplaintPersistenceAdapter(
            ComplaintJpaRepository complaintJpaRepository,
            CustomerJpaRepository customerJpaRepository,
            ComplaintStatusJpaRepository complaintStatusJpaRepository,
            CategoryJpaRepository categoryJpaRepository,
            ComplaintPersistenceMapper complaintPersistenceMapper,
            ResilientExecutor resilientExecutor
    ) {
        this.complaintJpaRepository = complaintJpaRepository;
        this.customerJpaRepository = customerJpaRepository;
        this.complaintStatusJpaRepository = complaintStatusJpaRepository;
        this.categoryJpaRepository = categoryJpaRepository;
        this.complaintPersistenceMapper = complaintPersistenceMapper;
        this.resilientExecutor = resilientExecutor;
    }

    @Override
    @Transactional
    public Complaint save(Complaint complaint) {
        return resilientExecutor.executeSupplier(ResilienceProfile.PERSISTENCE, () -> {
            CustomerEntity customerEntity = customerJpaRepository.save(
                    complaintPersistenceMapper.toCustomerEntity(complaint.customer())
            );
            ComplaintStatusEntity statusEntity = complaintStatusJpaRepository.findById(complaint.status().id())
                    .orElseThrow(() -> new ReferenceDataNotFoundException("O status de referência da reclamação não foi encontrado."));
            Set<Long> categoryIds = complaint.categories().stream()
                    .map(Category::id)
                    .collect(Collectors.toSet());
            Set<CategoryEntity> categoryEntities = Set.copyOf(categoryJpaRepository.findAllById(categoryIds));
            ComplaintEntity savedEntity = complaintJpaRepository.save(
                    complaintPersistenceMapper.toEntity(complaint, customerEntity, statusEntity, categoryEntities)
            );
            return complaintPersistenceMapper.toDomain(savedEntity);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<Complaint> search(SearchComplaintsQuery query) {
        return resilientExecutor.executeSupplier(
                ResilienceProfile.PERSISTENCE,
                () -> complaintJpaRepository.findAll(ComplaintSpecifications.from(query), COMPLAINT_DATE_DESC).stream()
                        .map(complaintPersistenceMapper::toDomain)
                        .toList()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<Complaint> findNonResolvedComplaintsByComplaintDate(LocalDate complaintDate) {
        return resilientExecutor.executeSupplier(
                ResilienceProfile.PERSISTENCE,
                () -> complaintJpaRepository.findNonResolvedByComplaintDate(
                                complaintDate,
                                ComplaintStatus.RESOLVED.id()
                        ).stream()
                        .map(complaintPersistenceMapper::toDomain)
                        .toList()
        );
    }
}
