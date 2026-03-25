package com.complaintmanagementservice.adapters.in.messaging.mapper;

import com.complaintmanagementservice.adapters.in.messaging.dto.CreateComplaintQueueMessage;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class QueueMessageMapperTest {

    @Test
    void shouldMapQueueMessageToCommand() {
        CreateComplaintQueueMessageMapper mapper = new CreateComplaintQueueMessageMapper();
        CreateComplaintQueueMessage message = new CreateComplaintQueueMessage(
                "52998224725",
                "Maria Silva",
                LocalDate.of(1990, 6, 15),
                "maria@example.com",
                LocalDate.of(2026, 3, 20),
                "Descricao"
        );

        var command = mapper.toCommand(message);

        assertThat(command.customerCpf().value()).isEqualTo("52998224725");
        assertThat(command.customerName().value()).isEqualTo("Maria Silva");
        assertThat(command.documentUrls()).isEmpty();
    }
}
