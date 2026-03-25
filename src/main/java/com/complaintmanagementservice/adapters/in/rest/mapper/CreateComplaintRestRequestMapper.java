package com.complaintmanagementservice.adapters.in.rest.mapper;

import com.complaintmanagementservice.adapters.in.rest.dto.CreateComplaintRestRequest;
import com.complaintmanagementservice.application.command.CreateComplaintCommand;
import com.complaintmanagementservice.domain.model.ComplaintText;
import com.complaintmanagementservice.domain.model.Cpf;
import com.complaintmanagementservice.domain.model.CustomerName;
import com.complaintmanagementservice.domain.model.DocumentUrl;
import com.complaintmanagementservice.domain.model.EmailAddress;

import java.util.List;

public class CreateComplaintRestRequestMapper {

    public CreateComplaintCommand toCommand(CreateComplaintRestRequest request) {
        List<DocumentUrl> documentUrls = request.documentUrls() == null
                ? List.of()
                : request.documentUrls().stream().map(DocumentUrl::new).toList();
        return new CreateComplaintCommand(
                new Cpf(request.customer().cpf()),
                new CustomerName(request.customer().name()),
                request.customer().birthDate(),
                new EmailAddress(request.customer().email()),
                request.complaintCreatedDate(),
                new ComplaintText(request.complaintText()),
                documentUrls
        );
    }
}
