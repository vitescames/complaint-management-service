package com.complaintmanagementservice.application.command;

import com.complaintmanagementservice.application.exception.ApplicationValidationException;
import com.complaintmanagementservice.domain.model.ComplaintText;
import com.complaintmanagementservice.domain.model.Cpf;
import com.complaintmanagementservice.domain.model.CustomerName;
import com.complaintmanagementservice.domain.model.DocumentUrl;
import com.complaintmanagementservice.domain.model.EmailAddress;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public record CreateComplaintCommand(
        Cpf customerCpf,
        CustomerName customerName,
        LocalDate customerBirthDate,
        EmailAddress customerEmail,
        LocalDate complaintCreatedDate,
        ComplaintText complaintText,
        List<DocumentUrl> documentUrls
) {

    public CreateComplaintCommand {
        Objects.requireNonNull(customerCpf, "customerCpf must not be null");
        Objects.requireNonNull(customerName, "customerName must not be null");
        Objects.requireNonNull(customerBirthDate, "customerBirthDate must not be null");
        Objects.requireNonNull(customerEmail, "customerEmail must not be null");
        Objects.requireNonNull(complaintCreatedDate, "complaintCreatedDate must not be null");
        Objects.requireNonNull(complaintText, "complaintText must not be null");
        documentUrls = List.copyOf(Objects.requireNonNullElse(documentUrls, List.of()));
        if (complaintCreatedDate.isAfter(LocalDate.now())) {
            throw new ApplicationValidationException("Complaint created date cannot be in the future");
        }
    }
}
