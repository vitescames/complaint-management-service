package com.complaintmanagementservice.application.query;

import com.complaintmanagementservice.application.exception.ApplicationValidationException;
import com.complaintmanagementservice.domain.model.ComplaintStatus;
import com.complaintmanagementservice.domain.model.Cpf;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record SearchComplaintsQuery(
        Optional<Cpf> customerCpf,
        List<String> categoryNames,
        List<ComplaintStatus> statuses,
        Optional<LocalDate> startDate,
        Optional<LocalDate> endDate
) {

    public SearchComplaintsQuery {
        customerCpf = normalizeOptional(customerCpf);
        startDate = normalizeOptional(startDate);
        endDate = normalizeOptional(endDate);
        categoryNames = List.copyOf(Objects.requireNonNullElse(categoryNames, List.of()))
                .stream()
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
        statuses = List.copyOf(Objects.requireNonNullElse(statuses, List.of()));
        if (startDate.isPresent() && endDate.isPresent() && startDate.get().isAfter(endDate.get())) {
            throw new ApplicationValidationException("Start date must be before or equal to end date");
        }
    }

    private static <T> Optional<T> normalizeOptional(Optional<T> value) {
        return Objects.requireNonNullElse(value, Optional.empty());
    }
}
