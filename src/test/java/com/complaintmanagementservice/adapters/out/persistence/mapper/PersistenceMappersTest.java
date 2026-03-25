package com.complaintmanagementservice.adapters.out.persistence.mapper;

import com.complaintmanagementservice.TestFixtures;
import com.complaintmanagementservice.adapters.out.persistence.entity.CategoryEntity;
import com.complaintmanagementservice.adapters.out.persistence.entity.CategoryKeywordEntity;
import com.complaintmanagementservice.adapters.out.persistence.entity.ComplaintDocumentEntity;
import com.complaintmanagementservice.adapters.out.persistence.entity.ComplaintEntity;
import com.complaintmanagementservice.adapters.out.persistence.entity.ComplaintStatusEntity;
import com.complaintmanagementservice.adapters.out.persistence.entity.CustomerEntity;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PersistenceMappersTest {

    private final CategoryPersistenceMapper categoryPersistenceMapper = new CategoryPersistenceMapper();
    private final ComplaintPersistenceMapper complaintPersistenceMapper = new ComplaintPersistenceMapper(categoryPersistenceMapper);

    @Test
    void shouldMapCategoryAndComplaintEntitiesToDomain() {
        CategoryEntity categoryEntity = new CategoryEntity(4L, "acesso");
        CategoryKeywordEntity keywordEntity = new CategoryKeywordEntity(11L, categoryEntity, "login");
        categoryEntity.getKeywords().add(keywordEntity);
        CustomerEntity customerEntity = new CustomerEntity("52998224725", "Maria Silva", LocalDate.of(1990, 6, 15), "maria@example.com");
        ComplaintStatusEntity statusEntity = new ComplaintStatusEntity(1, "PENDING");
        ComplaintEntity complaintEntity = new ComplaintEntity(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                customerEntity,
                statusEntity,
                LocalDate.of(2026, 3, 20),
                "Texto",
                Instant.parse("2026-03-23T10:15:30Z"),
                Set.of(categoryEntity)
        );
        complaintEntity.replaceDocuments(List.of(new ComplaintDocumentEntity("https://example.com/doc-1")));

        assertThat(categoryPersistenceMapper.toDomain(categoryEntity).keywords()).hasSize(1);
        assertThat(categoryPersistenceMapper.toComplaintCategory(categoryEntity).keywords()).isEmpty();
        assertThat(complaintPersistenceMapper.toDomain(complaintEntity).customer().cpf().value()).isEqualTo("52998224725");
        assertThat(complaintEntity.getDocuments()).hasSize(1);
        assertThat(complaintEntity.getDocuments().get(0).getComplaint()).isEqualTo(complaintEntity);
    }

    @Test
    void shouldMapDomainComplaintToEntities() {
        CustomerEntity customerEntity = complaintPersistenceMapper.toCustomerEntity(TestFixtures.customer());
        ComplaintStatusEntity statusEntity = new ComplaintStatusEntity(1, "PENDING");
        CategoryEntity categoryEntity = new CategoryEntity(4L, "acesso");
        ComplaintEntity entity = complaintPersistenceMapper.toEntity(
                TestFixtures.complaint(),
                customerEntity,
                statusEntity,
                Set.of(categoryEntity)
        );

        assertThat(customerEntity.getCpf()).isEqualTo("52998224725");
        assertThat(customerEntity.getName()).isEqualTo("Maria Silva");
        assertThat(customerEntity.getBirthDate()).isEqualTo(LocalDate.of(1990, 6, 15));
        assertThat(customerEntity.getEmail()).isEqualTo("maria.silva@example.com");
        assertThat(statusEntity.getId()).isEqualTo(1);
        assertThat(statusEntity.getName()).isEqualTo("PENDING");
        assertThat(categoryEntity.getId()).isEqualTo(4L);
        assertThat(categoryEntity.getName()).isEqualTo("acesso");
        assertThat(entity.getId()).isEqualTo(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        assertThat(entity.getComplaintText()).isEqualTo("Nao consigo acessar o app e a fatura esta indevida");
        assertThat(entity.getCategories()).containsExactly(categoryEntity);
        assertThat(entity.getDocuments().get(0).getDocumentUrl()).isEqualTo("https://example.com/doc-1");
        assertThat(entity.getStatus()).isEqualTo(statusEntity);
        assertThat(entity.getCustomer()).isEqualTo(customerEntity);
        assertThat(entity.getComplaintDate()).isEqualTo(LocalDate.of(2026, 3, 20));
        assertThat(entity.getRegisteredAt()).isEqualTo(Instant.parse("2026-03-23T10:15:30Z"));
        assertThat(entity.getDocuments().get(0).getComplaint()).isEqualTo(entity);
        assertThat(entity.getDocuments().get(0).getId()).isNull();
        assertThat(entity.getDocuments().get(0).getComplaint()).isNotNull();
        assertThat(keywordEntityReference(categoryEntity)).isNull();
    }

    private CategoryEntity keywordEntityReference(CategoryEntity categoryEntity) {
        return categoryEntity.getKeywords().stream().findFirst().map(CategoryKeywordEntity::getCategory).orElse(null);
    }
}
