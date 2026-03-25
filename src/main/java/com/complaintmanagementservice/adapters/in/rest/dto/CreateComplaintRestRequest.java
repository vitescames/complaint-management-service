package com.complaintmanagementservice.adapters.in.rest.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record CreateComplaintRestRequest(
        @NotNull @Valid CustomerPayload customer,
        @NotNull LocalDate complaintCreatedDate,
        @NotBlank @Size(max = 4000) String complaintText,
        List<@NotBlank @Pattern(regexp = "https?://.+") String> documentUrls
) {

    public record CustomerPayload(
            @NotBlank @Pattern(regexp = "\\d{3}\\.?\\d{3}\\.?\\d{3}-?\\d{2}|\\d{11}") String cpf,
            @NotBlank @Size(max = 120) String name,
            @NotNull LocalDate birthDate,
            @NotBlank @Email String email
    ) {
    }
}
