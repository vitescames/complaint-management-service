package com.complaintmanagementservice.application.usecase;

import com.complaintmanagementservice.application.exception.BusinessRuleViolationException;
import com.complaintmanagementservice.application.port.in.SearchComplaintsUseCase;
import com.complaintmanagementservice.application.port.out.ComplaintRepositoryPort;
import com.complaintmanagementservice.application.query.SearchComplaintsQuery;
import com.complaintmanagementservice.domain.model.Complaint;
import com.complaintmanagementservice.domain.model.ComplaintStatus;
import com.complaintmanagementservice.domain.model.Cpf;

import java.util.ArrayList;
import java.util.List;

public class SearchComplaintsUseCaseImpl implements SearchComplaintsUseCase {

    private final ComplaintRepositoryPort complaintRepositoryPort;

    public SearchComplaintsUseCaseImpl(ComplaintRepositoryPort complaintRepositoryPort) {
        this.complaintRepositoryPort = complaintRepositoryPort;
    }

    @Override
    public List<Complaint> search(SearchComplaintsQuery query) {
        SearchComplaintsQuery normalizedQuery = normalize(query);
        validate(normalizedQuery);
        return complaintRepositoryPort.search(normalizedQuery);
    }

    private SearchComplaintsQuery normalize(SearchComplaintsQuery query) {
        return SearchComplaintsQuery.builder()
                .customerCpf(normalizeValue(query.customerCpf()))
                .categoryNames(normalizeValues(query.categoryNames()))
                .statusIds(filterNullStatusIds(query.statusIds()))
                .startDate(query.startDate())
                .endDate(query.endDate())
                .build();
    }

    private void validate(SearchComplaintsQuery query) {
        if (query.customerCpf() != null) {
            new Cpf(query.customerCpf());
        }
        query.statusIds().forEach(ComplaintStatus::fromId);

        if (query.startDate() != null && query.endDate() != null && query.startDate().isAfter(query.endDate())) {
            throw new BusinessRuleViolationException("A data inicial deve ser menor ou igual à data final.");
        }
    }

    private List<String> normalizeValues(List<String> values) {
        List<String> normalizedValues = new ArrayList<>();
        for (String value : values) {
            String normalizedValue = normalizeValue(value);
            if (normalizedValue != null) {
                normalizedValues.add(normalizedValue);
            }
        }
        return normalizedValues;
    }

    private List<Integer> filterNullStatusIds(List<Integer> statusIds) {
        List<Integer> normalizedStatusIds = new ArrayList<>();
        for (Integer statusId : statusIds) {
            if (statusId != null) {
                normalizedStatusIds.add(statusId);
            }
        }
        return normalizedStatusIds;
    }

    private String normalizeValue(String value) {
        if (value == null) {
            return null;
        }

        String normalizedValue = value.trim();
        return normalizedValue.isEmpty() ? null : normalizedValue;
    }
}
