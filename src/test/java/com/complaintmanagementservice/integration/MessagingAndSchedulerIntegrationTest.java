package com.complaintmanagementservice.integration;

import com.complaintmanagementservice.adapters.in.messaging.dto.CreateComplaintQueueMessage;
import com.complaintmanagementservice.adapters.out.messaging.dto.ComplaintCreatedQueueMessage;
import com.complaintmanagementservice.adapters.out.messaging.dto.ComplaintSlaWarningQueueMessage;
import com.complaintmanagementservice.adapters.out.persistence.entity.ComplaintEntity;
import com.complaintmanagementservice.domain.model.ComplaintStatus;
import jakarta.jms.Message;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.task.scheduling.enabled=false")
@Import(IntegrationTestConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class MessagingAndSchedulerIntegrationTest extends IntegrationTestSupport {

    @Test
    void shouldConsumeReceivedQueueMessageCreateComplaintAndPublishCreatedEvent() {
        CreateComplaintQueueMessage message = validQueueMessage(
        );

        jmsTemplate.convertAndSend(messagingProperties.queues().complaintReceived(), message);

        awaitCondition(() -> complaintJpaRepository.count() == 1L, "Complaint was not persisted after queue consumption");

        UUID complaintId = complaintJpaRepository.findAll().get(0).getId();
        ComplaintEntity complaintEntity = loadComplaintEntity(complaintId);
        ComplaintCreatedQueueMessage createdEvent = awaitCreatedEvent();

        assertThat(customerJpaRepository.findById("52998224725")).isPresent();
        assertThat(complaintEntity.getStatus().getId()).isEqualTo(ComplaintStatus.PENDING.id());
        assertThat(complaintEntity.getCategories()).extracting("name")
                .containsExactlyInAnyOrder("acesso", "aplicativo", "cobrança", "fraude");
        assertThat(complaintEntity.getDocuments()).isEmpty();
        assertThat(createdEvent.complaintId()).isEqualTo(complaintEntity.getId());
        assertThat(createdEvent.createdAt()).isEqualTo(REFERENCE_INSTANT);
    }

    @Test
    void shouldRouteInvalidQueueMessagesToDeadLetterQueue() {
        CreateComplaintQueueMessage invalidMessage = new CreateComplaintQueueMessage(
                null,
                "Maria da Silva",
                LocalDate.of(1990, 6, 15),
                "maria.silva@example.com",
                LocalDate.of(2026, 3, 20),
                "Mensagem inválida"
        );

        jmsTemplate.convertAndSend(messagingProperties.queues().complaintReceived(), invalidMessage);

        Message deadLetterMessage = awaitQueueMessage(dlqName(messagingProperties.queues().complaintReceived()));

        assertThat(deadLetterMessage).isNotNull();
        assertThat(complaintJpaRepository.count()).isZero();
    }

    @Test
    void shouldPublishSlaWarningWhenSchedulerFindsDueComplaint() {
        var dueComplaint = persistComplaint(
                "52998224725",
                "Maria da Silva",
                "maria.silva@example.com",
                LocalDate.of(1990, 6, 15),
                LocalDate.of(2026, 3, 16),
                "Reclamação pendente próxima do SLA",
                ComplaintStatus.PROCESSING,
                Set.of("acesso"),
                List.of()
        );

        slaWarningScheduler.publishWarnings();

        ComplaintSlaWarningQueueMessage warningEvent = awaitSlaWarningEvent();

        assertThat(warningEvent.complaintId()).isEqualTo(dueComplaint.id().value());
        assertThat(warningEvent.slaDeadlineDate()).isEqualTo(LocalDate.of(2026, 3, 26));
    }

    @Test
    void shouldNotPublishSlaWarningForResolvedComplaint() {
        persistComplaint(
                "52998224725",
                "Maria da Silva",
                "maria.silva@example.com",
                LocalDate.of(1990, 6, 15),
                LocalDate.of(2026, 3, 16),
                "Reclamação resolvida",
                ComplaintStatus.RESOLVED,
                Set.of("acesso"),
                List.of()
        );

        slaWarningScheduler.publishWarnings();

        assertThat(jmsTemplate.receive(messagingProperties.queues().complaintSlaWarning())).isNull();
    }

    @Test
    void shouldNotPublishSlaWarningOutsideWarningWindow() {
        persistComplaint(
                "11144477735",
                "João Souza",
                "joao.souza@example.com",
                LocalDate.of(1988, 2, 10),
                LocalDate.of(2026, 3, 15),
                "Reclamação fora da janela do SLA",
                ComplaintStatus.PROCESSING,
                Set.of("seguros"),
                List.of()
        );

        slaWarningScheduler.publishWarnings();

        assertThat(jmsTemplate.receive(messagingProperties.queues().complaintSlaWarning())).isNull();
    }
}
