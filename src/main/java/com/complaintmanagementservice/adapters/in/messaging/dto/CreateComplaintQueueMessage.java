package com.complaintmanagementservice.adapters.in.messaging.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateComplaintQueueMessage(
        @NotBlank @Pattern(regexp = "\\d{11}") String customerDocument,
        @NotBlank @Size(max = 120) String customerFullName,
        @NotNull LocalDate customerBirthDate,
        @NotBlank @Email String customerEmailAddress,
        @NotNull LocalDate occurrenceDate,
        @NotBlank @Size(max = 4000) String description
) {
}
