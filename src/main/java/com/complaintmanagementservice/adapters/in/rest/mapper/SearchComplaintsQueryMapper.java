package com.complaintmanagementservice.adapters.in.rest.mapper;

import com.complaintmanagementservice.application.query.SearchComplaintsQuery;
import com.complaintmanagementservice.domain.model.ComplaintStatus;
import com.complaintmanagementservice.domain.model.Cpf;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class SearchComplaintsQueryMapper {

    public SearchComplaintsQuery toQuery(
            String customerCpf,
            List<String> categories,
            List<Integer> statusIds,
            LocalDate startDate,
            LocalDate endDate
    ) {
        Optional<Cpf> cpf = customerCpf == null || customerCpf.isBlank() ? Optional.empty() : Optional.of(new Cpf(customerCpf));
        List<ComplaintStatus> statuses = statusIds == null
                ? List.of()
                : statusIds.stream().map(ComplaintStatus::fromId).toList();
        return new SearchComplaintsQuery(
                cpf,
                categories == null ? List.of() : categories,
                statuses,
                Optional.ofNullable(startDate),
                Optional.ofNullable(endDate)
        );
    }
}
