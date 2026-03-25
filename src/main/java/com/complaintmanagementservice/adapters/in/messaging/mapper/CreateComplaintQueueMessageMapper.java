package com.complaintmanagementservice.adapters.in.messaging.mapper;

import com.complaintmanagementservice.adapters.in.messaging.dto.CreateComplaintQueueMessage;
import com.complaintmanagementservice.application.command.CreateComplaintCommand;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CreateComplaintQueueMessageMapper {

    public CreateComplaintCommand toCommand(CreateComplaintQueueMessage message) {
        return CreateComplaintCommand.builder()
                .customerCpf(message.customerDocument())
                .customerName(message.customerFullName())
                .customerBirthDate(message.customerBirthDate())
                .customerEmail(message.customerEmailAddress())
                .complaintCreatedDate(message.occurrenceDate())
                .complaintText(message.description())
                .documentUrls(List.of())
                .build();
    }
}
