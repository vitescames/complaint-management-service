package com.complaintmanagementservice.adapters.in.rest.mapper;

import com.complaintmanagementservice.adapters.in.rest.dto.ComplaintSearchResponse;
import com.complaintmanagementservice.adapters.in.rest.dto.CreateComplaintRestResponse;
import com.complaintmanagementservice.domain.model.Complaint;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class ComplaintResponseMapper {

    public CreateComplaintRestResponse toCreateResponse(Complaint complaint) {
        return new CreateComplaintRestResponse(
                complaint.id().value().toString(),
                complaint.status().id(),
                complaint.status().name()
        );
    }

    public ComplaintSearchResponse toSearchResponse(Complaint complaint) {
        return new ComplaintSearchResponse(
                complaint.id().value().toString(),
                complaint.complaintDate(),
                complaint.complaintText().value(),
                new ComplaintSearchResponse.StatusPayload(complaint.status().id(), complaint.status().name()),
                new ComplaintSearchResponse.CustomerPayload(
                        complaint.customer().cpf().value(),
                        complaint.customer().name().value(),
                        complaint.customer().birthDate(),
                        complaint.customer().emailAddress().value()
                ),
                complaint.categories().stream()
                        .sorted(Comparator.comparing(category -> category.name().toLowerCase()))
                        .map(category -> new ComplaintSearchResponse.CategoryPayload(category.id(), category.name()))
                        .toList(),
                complaint.documentUrls().stream().map(documentUrl -> documentUrl.value()).toList(),
                complaint.registeredAt()
        );
    }

    public List<ComplaintSearchResponse> toSearchResponses(List<Complaint> complaints) {
        return complaints.stream().map(this::toSearchResponse).toList();
    }
}
