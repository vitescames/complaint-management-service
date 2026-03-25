package com.complaintmanagementservice.adapters.in.messaging.mapper;

import com.complaintmanagementservice.adapters.in.messaging.dto.CreateComplaintQueueMessage;
import com.complaintmanagementservice.application.command.CreateComplaintCommand;
import com.complaintmanagementservice.domain.model.ComplaintText;
import com.complaintmanagementservice.domain.model.Cpf;
import com.complaintmanagementservice.domain.model.CustomerName;
import com.complaintmanagementservice.domain.model.EmailAddress;

import java.util.List;

public class CreateComplaintQueueMessageMapper {

    public CreateComplaintCommand toCommand(CreateComplaintQueueMessage message) {
        return new CreateComplaintCommand(
                new Cpf(message.customerDocument()),
                new CustomerName(message.customerFullName()),
                message.customerBirthDate(),
                new EmailAddress(message.customerEmailAddress()),
                message.occurrenceDate(),
                new ComplaintText(message.description()),
                List.of()
        );
    }
}
