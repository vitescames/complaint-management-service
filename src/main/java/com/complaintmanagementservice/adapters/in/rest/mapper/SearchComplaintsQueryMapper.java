package com.complaintmanagementservice.adapters.in.rest.mapper;

import com.complaintmanagementservice.application.query.SearchComplaintsQuery;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
public class SearchComplaintsQueryMapper {

    public SearchComplaintsQuery toQuery(
            String customerCpf,
            List<String> categories,
            List<Integer> statusIds,
            LocalDate startDate,
            LocalDate endDate
    ) {
        return SearchComplaintsQuery.builder()
                .customerCpf(normalize(customerCpf))
                .categoryNames(normalizeValues(categories))
                .statusIds(filterNullStatusIds(statusIds))
                .startDate(startDate)
                .endDate(endDate)
                .build();
    }

    private List<String> normalizeValues(List<String> values) {
        if (values == null) {
            return List.of();
        }

        List<String> normalizedValues = new ArrayList<>();
        for (String value : values) {
            String normalizedValue = normalize(value);
            if (normalizedValue != null) {
                normalizedValues.add(normalizedValue);
            }
        }
        return normalizedValues;
    }

    private List<Integer> filterNullStatusIds(List<Integer> statusIds) {
        if (statusIds == null) {
            return List.of();
        }

        List<Integer> normalizedStatusIds = new ArrayList<>();
        for (Integer statusId : statusIds) {
            if (statusId != null) {
                normalizedStatusIds.add(statusId);
            }
        }
        return normalizedStatusIds;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }

        String normalizedValue = value.trim();
        return normalizedValue.isEmpty() ? null : normalizedValue;
    }
}
