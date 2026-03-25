package com.complaintmanagementservice.adapters.in.messaging;

import com.complaintmanagementservice.adapters.in.messaging.dto.CreateComplaintQueueMessage;
import com.complaintmanagementservice.adapters.in.messaging.mapper.CreateComplaintQueueMessageMapper;
import com.complaintmanagementservice.application.port.in.CreateComplaintUseCase;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.springframework.jms.annotation.JmsListener;

public class ComplaintReceivedListener {

    private final CreateComplaintUseCase createComplaintUseCase;
    private final CreateComplaintQueueMessageMapper createComplaintQueueMessageMapper;
    private final Validator validator;

    public ComplaintReceivedListener(
            CreateComplaintUseCase createComplaintUseCase,
            CreateComplaintQueueMessageMapper createComplaintQueueMessageMapper,
            Validator validator
    ) {
        this.createComplaintUseCase = createComplaintUseCase;
        this.createComplaintQueueMessageMapper = createComplaintQueueMessageMapper;
        this.validator = validator;
    }

    @JmsListener(destination = "${application.messaging.queues.complaint-received}", containerFactory = "complaintListenerContainerFactory")
    public void receive(CreateComplaintQueueMessage message) {
        var violations = validator.validate(message);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
        createComplaintUseCase.create(createComplaintQueueMessageMapper.toCommand(message));
    }
}
