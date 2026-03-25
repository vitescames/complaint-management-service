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
        @NotNull(message = "Os dados do cliente sao obrigatorios") @Valid CustomerPayload customer,
        @NotNull(message = "A data da reclamacao e obrigatoria") LocalDate complaintCreatedDate,
        @NotBlank(message = "O texto da reclamacao e obrigatorio")
        @Size(max = 4000, message = "O texto da reclamacao deve ter no maximo 4000 caracteres")
        String complaintText,
        List<
                @NotBlank(message = "A URL do documento e obrigatoria")
                @Pattern(regexp = "https?://.+", message = "A URL do documento deve ser HTTP ou HTTPS valida")
                String> documentUrls
) {

    public record CustomerPayload(
            @NotBlank(message = "O CPF do cliente e obrigatorio")
            @Pattern(regexp = "\\d{3}\\.?\\d{3}\\.?\\d{3}-?\\d{2}|\\d{11}", message = "O CPF do cliente e invalido")
            String cpf,
            @NotBlank(message = "O nome do cliente e obrigatorio")
            @Size(max = 120, message = "O nome do cliente deve ter no maximo 120 caracteres")
            String name,
            @NotNull(message = "A data de nascimento do cliente e obrigatoria")
            LocalDate birthDate,
            @NotBlank(message = "O e-mail do cliente e obrigatorio")
            @Email(message = "Formato de e-mail invalido")
            String email
    ) {
    }
}
