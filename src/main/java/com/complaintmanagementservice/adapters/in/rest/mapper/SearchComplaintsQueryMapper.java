package com.complaintmanagementservice.adapters.in.rest.mapper;

import com.complaintmanagementservice.application.query.SearchComplaintsQuery;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
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
                .customerCpf(customerCpf)
                .categoryNames(categories == null ? List.of() : categories)
                .statusIds(statusIds == null ? List.of() : statusIds)
                .startDate(startDate)
                .endDate(endDate)
                .build();
    }
}
