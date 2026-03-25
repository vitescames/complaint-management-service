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
import com.complaintmanagementservice.domain.model.Complaint;
import com.complaintmanagementservice.domain.model.ComplaintStatus;
import com.complaintmanagementservice.infrastructure.resilience.ResilienceProfile;
import com.complaintmanagementservice.infrastructure.resilience.ResilientExecutor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ComplaintPersistenceAdapter implements ComplaintRepositoryPort {

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
    public Complaint save(Complaint complaint) {
        return resilientExecutor.executeSupplier(ResilienceProfile.PERSISTENCE, () -> {
            CustomerEntity customerEntity = customerJpaRepository.save(complaintPersistenceMapper.toCustomerEntity(complaint.customer()));
            ComplaintStatusEntity statusEntity = complaintStatusJpaRepository.findById(complaint.status().id())
                    .orElseThrow(() -> new ReferenceDataNotFoundException("Complaint status reference data is missing"));
            Set<CategoryEntity> categoryEntities = Set.copyOf(categoryJpaRepository.findAllById(
                    complaint.categories().stream().map(category -> category.id()).toList()
            ));
            ComplaintEntity savedEntity = complaintJpaRepository.save(
                    complaintPersistenceMapper.toEntity(complaint, customerEntity, statusEntity, categoryEntities)
            );
            return complaintPersistenceMapper.toDomain(savedEntity);
        });
    }

    @Override
    public List<Complaint> search(SearchComplaintsQuery query) {
        return resilientExecutor.executeSupplier(
                ResilienceProfile.PERSISTENCE,
                () -> complaintJpaRepository.findAll(buildSpecification(query), Sort.by(Sort.Direction.DESC, "complaintDate"))
                        .stream()
                        .map(complaintPersistenceMapper::toDomain)
                        .toList()
        );
    }

    @Override
    public List<Complaint> findNonResolvedComplaintsCreatedOn(LocalDate complaintDate) {
        return resilientExecutor.executeSupplier(
                ResilienceProfile.PERSISTENCE,
                () -> complaintJpaRepository.findByComplaintDateAndStatusIdNotOrderByComplaintDateDesc(
                                complaintDate,
                                ComplaintStatus.RESOLVED.id()
                        )
                        .stream()
                        .map(complaintPersistenceMapper::toDomain)
                        .toList()
        );
    }

    private Specification<ComplaintEntity> buildSpecification(SearchComplaintsQuery query) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            if (query.customerCpf().isPresent()) {
                predicates.add(criteriaBuilder.equal(root.get("customer").get("cpf"), query.customerCpf().get().value()));
            }
            if (!query.categoryNames().isEmpty()) {
                predicates.add(root.join("categories").get("name").in(query.categoryNames()));
                criteriaQuery.distinct(true);
            }
            if (!query.statuses().isEmpty()) {
                predicates.add(root.get("status").get("id").in(query.statuses().stream().map(ComplaintStatus::id).toList()));
            }
            query.startDate().ifPresent(startDate ->
                    predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("complaintDate"), startDate))
            );
            query.endDate().ifPresent(endDate ->
                    predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("complaintDate"), endDate))
            );

            return criteriaBuilder.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }
}
