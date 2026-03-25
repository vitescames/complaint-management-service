package com.complaintmanagementservice.adapters.in.rest.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record ComplaintSearchResponse(
        String complaintId,
        LocalDate complaintCreatedDate,
        String complaintText,
        StatusPayload status,
        CustomerPayload customer,
        List<CategoryPayload> categories,
        List<String> documentUrls,
        Instant registeredAt
) {

    public record StatusPayload(int id, String name) {
    }

    public record CustomerPayload(String cpf, String name, LocalDate birthDate, String email) {
    }

    public record CategoryPayload(Long id, String name) {
    }
}
