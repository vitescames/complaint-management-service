package com.complaintmanagementservice.adapters.out.persistence;

import com.complaintmanagementservice.TestFixtures;
import com.complaintmanagementservice.adapters.out.persistence.entity.ComplaintEntity;
import com.complaintmanagementservice.application.query.SearchComplaintsQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ComplaintPersistenceSpecificationTest {

    @Test
    void shouldBuildSpecificationUsingAllFilters() {
        SearchComplaintsQuery query = TestFixtures.searchQuery();
        Specification<ComplaintEntity> specification = ComplaintSpecifications.from(query);

        Root root = mock(Root.class);
        CriteriaQuery criteriaQuery = mock(CriteriaQuery.class);
        CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);
        Path customerPath = mock(Path.class);
        Path cpfPath = mock(Path.class);
        Join categoriesJoin = mock(Join.class);
        Path categoryNamePath = mock(Path.class);
        Path statusPath = mock(Path.class);
        Path statusIdPath = mock(Path.class);
        Path complaintDatePath = mock(Path.class);
        Predicate customerPredicate = mock(Predicate.class);
        Predicate categoryPredicate = mock(Predicate.class);
        Predicate statusPredicate = mock(Predicate.class);
        Predicate startDatePredicate = mock(Predicate.class);
        Predicate endDatePredicate = mock(Predicate.class);
        Predicate finalPredicate = mock(Predicate.class);

        when(root.get("customer")).thenReturn(customerPath);
        when(customerPath.get("cpf")).thenReturn(cpfPath);
        when(criteriaBuilder.equal(cpfPath, "52998224725")).thenReturn(customerPredicate);
        when(root.join("categories")).thenReturn(categoriesJoin);
        when(categoriesJoin.get("name")).thenReturn(categoryNamePath);
        when(categoryNamePath.in(query.categoryNames())).thenReturn(categoryPredicate);
        when(criteriaQuery.distinct(true)).thenReturn(criteriaQuery);
        when(root.get("status")).thenReturn(statusPath);
        when(statusPath.get("id")).thenReturn(statusIdPath);
        when(statusIdPath.in(List.of(1))).thenReturn(statusPredicate);
        when(root.get("complaintDate")).thenReturn(complaintDatePath);
        when(criteriaBuilder.greaterThanOrEqualTo(complaintDatePath, LocalDate.of(2026, 3, 1))).thenReturn(startDatePredicate);
        when(criteriaBuilder.lessThanOrEqualTo(complaintDatePath, LocalDate.of(2026, 3, 31))).thenReturn(endDatePredicate);
        when(criteriaBuilder.and(any(Predicate[].class))).thenReturn(finalPredicate);

        assertThat(specification.toPredicate(root, criteriaQuery, criteriaBuilder)).isEqualTo(finalPredicate);

        verify(criteriaQuery).distinct(true);
        verify(criteriaBuilder).equal(cpfPath, "52998224725");
        verify(criteriaBuilder).greaterThanOrEqualTo(complaintDatePath, LocalDate.of(2026, 3, 1));
        verify(criteriaBuilder).lessThanOrEqualTo(complaintDatePath, LocalDate.of(2026, 3, 31));
    }

    @Test
    void shouldBuildSpecificationWithoutFilters() {
        SearchComplaintsQuery query = SearchComplaintsQuery.builder().build();
        Specification<ComplaintEntity> specification = ComplaintSpecifications.from(query);

        Root root = mock(Root.class);
        CriteriaQuery criteriaQuery = mock(CriteriaQuery.class);
        CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);
        Predicate finalPredicate = mock(Predicate.class);

        when(criteriaBuilder.and(any(Predicate[].class))).thenReturn(finalPredicate);

        assertThat(specification.toPredicate(root, criteriaQuery, criteriaBuilder)).isEqualTo(finalPredicate);

        verify(root, never()).join(anyString());
        verify(root, never()).get(anyString());
        verify(criteriaQuery, never()).distinct(true);
    }
}
