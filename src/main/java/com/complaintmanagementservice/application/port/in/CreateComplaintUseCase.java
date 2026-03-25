package com.complaintmanagementservice.application.port.in;

import com.complaintmanagementservice.application.command.CreateComplaintCommand;
import com.complaintmanagementservice.domain.model.Complaint;

public interface CreateComplaintUseCase {

    Complaint create(CreateComplaintCommand command);
}
