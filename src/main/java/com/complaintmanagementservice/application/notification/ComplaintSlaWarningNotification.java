package com.complaintmanagementservice.application.notification;

import com.complaintmanagementservice.application.exception.RequestValidationException;
import com.complaintmanagementservice.domain.model.ComplaintId;

import java.time.LocalDate;

public record ComplaintSlaWarningNotification(ComplaintId complaintId, LocalDate slaDeadlineDate) {

    public ComplaintSlaWarningNotification {
        if (complaintId == null) {
            throw new RequestValidationException("O identificador da reclamacao e obrigatorio");
        }
        if (slaDeadlineDate == null) {
            throw new RequestValidationException("A data limite do SLA e obrigatoria");
        }
    }
}
