package com.complaintmanagementservice.adapters.in.messaging;

import com.complaintmanagementservice.adapters.in.messaging.dto.CreateComplaintQueueMessage;
import com.complaintmanagementservice.adapters.in.messaging.mapper.CreateComplaintQueueMessageMapper;
import com.complaintmanagementservice.application.port.in.CreateComplaintUseCase;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComplaintReceivedListenerTest {

    @Mock
    private CreateComplaintUseCase createComplaintUseCase;

    @Mock
    private Validator validator;

    @Mock
    private ConstraintViolation<CreateComplaintQueueMessage> violation;

    @Test
    void shouldConsumeValidQueueMessage() {
        ComplaintReceivedListener listener = new ComplaintReceivedListener(
                createComplaintUseCase,
                new CreateComplaintQueueMessageMapper(),
                validator
        );
        CreateComplaintQueueMessage message = new CreateComplaintQueueMessage(
                "52998224725",
                "Maria Silva",
                LocalDate.of(1990, 6, 15),
                "maria@example.com",
                LocalDate.of(2026, 3, 20),
                "Descricao"
        );
        when(validator.validate(message)).thenReturn(Set.of());

        listener.receive(message);

        verify(createComplaintUseCase).create(any());
    }

    @Test
    void shouldRejectInvalidQueueMessage() {
        ComplaintReceivedListener listener = new ComplaintReceivedListener(
                createComplaintUseCase,
                new CreateComplaintQueueMessageMapper(),
                validator
        );
        CreateComplaintQueueMessage message = new CreateComplaintQueueMessage(
                null,
                "Maria Silva",
                LocalDate.of(1990, 6, 15),
                "maria@example.com",
                LocalDate.of(2026, 3, 20),
                "Descricao"
        );
        when(validator.validate(message)).thenReturn(Set.of(violation));

        assertThatThrownBy(() -> listener.receive(message))
                .isInstanceOf(ConstraintViolationException.class);
    }
}
