package com.complaintmanagementservice.adapters.in.rest.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record CreateComplaintRestRequest(
        @NotNull(message = "Não pode ser nulo") @Valid CustomerPayload customer,
        @NotNull(message = "Não pode ser nulo") LocalDate complaintCreatedDate,
        @NotBlank(message = "Não pode ser nulo ou vazio")
        @Size(max = 4000, message = "Deve ter no máximo 4000 caracteres")
        String complaintText,
        List<@NotBlank(message = "Não pode ser nulo ou vazio")
             @Pattern(regexp = "https?://.+", message = "Formato inválido")
             String> documentUrls
) {

    public record CustomerPayload(
            @NotBlank(message = "Não pode ser nulo ou vazio")
            @Pattern(regexp = "\\d{3}\\.?\\d{3}\\.?\\d{3}-?\\d{2}|\\d{11}", message = "Formato inválido")
            String cpf,
            @NotBlank(message = "Não pode ser nulo ou vazio")
            @Size(max = 120, message = "Deve ter no máximo 120 caracteres")
            String name,
            @NotNull(message = "Não pode ser nulo")
            LocalDate birthDate,
            @NotBlank(message = "Não pode ser nulo ou vazio")
            @Email(message = "Formato inválido")
            String email
    ) {
    }
}
