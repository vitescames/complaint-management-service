package com.complaintmanagementservice.adapters.in.messaging.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateComplaintQueueMessage(
        @NotBlank(message = "O CPF do cliente e obrigatorio")
        @Pattern(regexp = "\\d{11}", message = "O CPF do cliente e invalido")
        String customerDocument,
        @NotBlank(message = "O nome do cliente e obrigatorio")
        @Size(max = 120, message = "O nome do cliente deve ter no maximo 120 caracteres")
        String customerFullName,
        @NotNull(message = "A data de nascimento do cliente e obrigatoria")
        LocalDate customerBirthDate,
        @NotBlank(message = "O e-mail do cliente e obrigatorio")
        @Email(message = "Formato de e-mail invalido")
        String customerEmailAddress,
        @NotNull(message = "A data da ocorrencia e obrigatoria")
        LocalDate occurrenceDate,
        @NotBlank(message = "A descricao da reclamacao e obrigatoria")
        @Size(max = 4000, message = "A descricao da reclamacao deve ter no maximo 4000 caracteres")
        String description
) {
}
