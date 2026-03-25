package com.complaintmanagementservice.adapters.out.persistence.entity;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;

import static org.assertj.core.api.Assertions.assertThat;

class PersistenceEntitiesCoverageTest {

    @Test
    void shouldInstantiateProtectedJpaConstructors() throws Exception {
        CategoryEntity categoryEntity = instantiate(CategoryEntity.class);
        CategoryKeywordEntity categoryKeywordEntity = instantiate(CategoryKeywordEntity.class);
        ComplaintEntity complaintEntity = instantiate(ComplaintEntity.class);
        ComplaintStatusEntity complaintStatusEntity = instantiate(ComplaintStatusEntity.class);
        CustomerEntity customerEntity = instantiate(CustomerEntity.class);

        assertThat(categoryEntity.getId()).isNull();
        assertThat(categoryEntity.getName()).isNull();
        assertThat(categoryEntity.getKeywords()).isEmpty();
        assertThat(categoryKeywordEntity.getId()).isNull();
        assertThat(categoryKeywordEntity.getCategory()).isNull();
        assertThat(categoryKeywordEntity.getValue()).isNull();
        assertThat(complaintEntity.getId()).isNull();
        assertThat(complaintEntity.getCustomer()).isNull();
        assertThat(complaintEntity.getStatus()).isNull();
        assertThat(complaintEntity.getComplaintDate()).isNull();
        assertThat(complaintEntity.getComplaintText()).isNull();
        assertThat(complaintEntity.getRegisteredAt()).isNull();
        assertThat(complaintEntity.getCategories()).isEmpty();
        assertThat(complaintEntity.getDocuments()).isEmpty();
        assertThat(complaintStatusEntity.getId()).isNull();
        assertThat(complaintStatusEntity.getName()).isNull();
        assertThat(customerEntity.getCpf()).isNull();
        assertThat(customerEntity.getName()).isNull();
        assertThat(customerEntity.getBirthDate()).isNull();
        assertThat(customerEntity.getEmail()).isNull();
    }

    private <T> T instantiate(Class<T> type) throws Exception {
        Constructor<T> constructor = type.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }
}
