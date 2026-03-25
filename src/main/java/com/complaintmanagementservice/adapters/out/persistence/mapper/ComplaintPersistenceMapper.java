package com.complaintmanagementservice.adapters.out.persistence.mapper;

import com.complaintmanagementservice.adapters.out.persistence.entity.CategoryEntity;
import com.complaintmanagementservice.adapters.out.persistence.entity.ComplaintDocumentEntity;
import com.complaintmanagementservice.adapters.out.persistence.entity.ComplaintEntity;
import com.complaintmanagementservice.adapters.out.persistence.entity.ComplaintStatusEntity;
import com.complaintmanagementservice.adapters.out.persistence.entity.CustomerEntity;
import com.complaintmanagementservice.domain.model.Complaint;
import com.complaintmanagementservice.domain.model.ComplaintId;
import com.complaintmanagementservice.domain.model.ComplaintStatus;
import com.complaintmanagementservice.domain.model.ComplaintText;
import com.complaintmanagementservice.domain.model.Cpf;
import com.complaintmanagementservice.domain.model.Customer;
import com.complaintmanagementservice.domain.model.CustomerName;
import com.complaintmanagementservice.domain.model.DocumentUrl;
import com.complaintmanagementservice.domain.model.EmailAddress;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ComplaintPersistenceMapper {

    private final CategoryPersistenceMapper categoryPersistenceMapper;

    public ComplaintPersistenceMapper(CategoryPersistenceMapper categoryPersistenceMapper) {
        this.categoryPersistenceMapper = categoryPersistenceMapper;
    }

    public CustomerEntity toCustomerEntity(Customer customer) {
        return new CustomerEntity(
                customer.cpf().value(),
                customer.name().value(),
                customer.birthDate(),
                customer.emailAddress().value()
        );
    }

    public ComplaintEntity toEntity(
            Complaint complaint,
            CustomerEntity customerEntity,
            ComplaintStatusEntity statusEntity,
            Set<CategoryEntity> categoryEntities
    ) {
        ComplaintEntity complaintEntity = new ComplaintEntity(
                complaint.id().value(),
                customerEntity,
                statusEntity,
                complaint.complaintDate(),
                complaint.complaintText().value(),
                complaint.registeredAt(),
                categoryEntities
        );
        complaintEntity.replaceDocuments(
                complaint.documentUrls().stream()
                        .map(documentUrl -> new ComplaintDocumentEntity(documentUrl.value()))
                        .toList()
        );
        return complaintEntity;
    }

    public Complaint toDomain(ComplaintEntity entity) {
        return Complaint.restore(
                new ComplaintId(entity.getId()),
                new Customer(
                        new Cpf(entity.getCustomer().getCpf()),
                        new CustomerName(entity.getCustomer().getName()),
                        entity.getCustomer().getBirthDate(),
                        new EmailAddress(entity.getCustomer().getEmail())
                ),
                entity.getComplaintDate(),
                new ComplaintText(entity.getComplaintText()),
                entity.getDocuments().stream().map(document -> new DocumentUrl(document.getDocumentUrl())).toList(),
                ComplaintStatus.fromId(entity.getStatus().getId()),
                entity.getCategories().stream()
                        .map(categoryPersistenceMapper::toComplaintCategory)
                        .collect(Collectors.toCollection(java.util.LinkedHashSet::new)),
                entity.getRegisteredAt()
        );
    }
}
