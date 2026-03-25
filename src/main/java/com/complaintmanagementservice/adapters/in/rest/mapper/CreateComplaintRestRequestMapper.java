package com.complaintmanagementservice.adapters.in.rest.mapper;

import com.complaintmanagementservice.adapters.in.rest.dto.CreateComplaintRestRequest;
import com.complaintmanagementservice.application.command.CreateComplaintCommand;
import org.springframework.stereotype.Component;

@Component
public class CreateComplaintRestRequestMapper {

    public CreateComplaintCommand toCommand(CreateComplaintRestRequest request) {
        return CreateComplaintCommand.builder()
                .customerCpf(request.customer().cpf())
                .customerName(request.customer().name())
                .customerBirthDate(request.customer().birthDate())
                .customerEmail(request.customer().email())
                .complaintCreatedDate(request.complaintCreatedDate())
                .complaintText(request.complaintText())
                .documentUrls(request.documentUrls())
                .build();
    }
}
