package com.complaintmanagementservice.adapters.in.messaging.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateComplaintQueueMessage(
        @NotBlank(message = "Não pode ser nulo ou vazio")
        @Pattern(regexp = "\\d{11}", message = "Formato inválido")
        String customerDocument,
        @NotBlank(message = "Não pode ser nulo ou vazio")
        @Size(max = 120, message = "Deve ter no máximo 120 caracteres")
        String customerFullName,
        @NotNull(message = "Não pode ser nulo")
        LocalDate customerBirthDate,
        @NotBlank(message = "Não pode ser nulo ou vazio")
        @Email(message = "Formato inválido")
        String customerEmailAddress,
        @NotNull(message = "Não pode ser nulo")
        LocalDate occurrenceDate,
        @NotBlank(message = "Não pode ser nulo ou vazio")
        @Size(max = 4000, message = "Deve ter no máximo 4000 caracteres")
        String description
) {
}
