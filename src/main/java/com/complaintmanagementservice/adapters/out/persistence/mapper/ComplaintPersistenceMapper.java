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
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
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
        return Complaint.builder()
                .id(new ComplaintId(entity.getId()))
                .customer(Customer.builder()
                        .cpf(new Cpf(entity.getCustomer().getCpf()))
                        .name(new CustomerName(entity.getCustomer().getName()))
                        .birthDate(entity.getCustomer().getBirthDate())
                        .emailAddress(new EmailAddress(entity.getCustomer().getEmail()))
                        .build())
                .complaintDate(entity.getComplaintDate())
                .complaintText(new ComplaintText(entity.getComplaintText()))
                .documentUrls(entity.getDocuments().stream().map(document -> new DocumentUrl(document.getDocumentUrl())).toList())
                .status(ComplaintStatus.fromId(entity.getStatus().getId()))
                .categories(entity.getCategories().stream()
                        .map(categoryPersistenceMapper::toComplaintCategory)
                        .collect(Collectors.toCollection(java.util.LinkedHashSet::new)))
                .registeredAt(entity.getRegisteredAt())
                .buildReconstituted();
    }
}
