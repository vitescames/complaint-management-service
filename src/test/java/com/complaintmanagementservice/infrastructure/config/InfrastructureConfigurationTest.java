package com.complaintmanagementservice.infrastructure.config;

import com.complaintmanagementservice.application.event.DomainEventObserver;
import com.complaintmanagementservice.application.event.DomainEventPublisher;
import com.complaintmanagementservice.application.port.in.CreateComplaintUseCase;
import com.complaintmanagementservice.application.port.in.PublishSlaWarningsUseCase;
import com.complaintmanagementservice.application.port.in.SearchComplaintsUseCase;
import com.complaintmanagementservice.application.port.out.CategoryCatalogPort;
import com.complaintmanagementservice.application.port.out.ComplaintRepositoryPort;
import com.complaintmanagementservice.application.port.out.ComplaintSlaWarningMessagePort;
import com.complaintmanagementservice.domain.event.DomainEvent;
import com.complaintmanagementservice.domain.service.ComplaintCategoryClassifier;
import com.complaintmanagementservice.domain.service.ComplaintSlaPolicy;
import com.complaintmanagementservice.infrastructure.messaging.JacksonTextMessageConverter;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import org.junit.jupiter.api.Test;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.ErrorHandler;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InfrastructureConfigurationTest {

    private final ApplicationConfiguration applicationConfiguration = new ApplicationConfiguration();
    private final MessagingConfiguration messagingConfiguration = new MessagingConfiguration();

    @Test
    void shouldCreateApplicationBeans() {
        ResilienceProperties resilienceProperties = new ResilienceProperties(
                new ResilienceProperties.ProfileSettings(2, 0, 2, 1, 50f, 1000),
                new ResilienceProperties.ProfileSettings(2, 0, 2, 1, 50f, 1000)
        );
        var resilientExecutor = applicationConfiguration.resilientExecutor(resilienceProperties);
        DomainEventObserver<DomainEvent> observer = new DomainEventObserver<>() {
            @Override
            public Class<DomainEvent> supportedEventType() {
                return DomainEvent.class;
            }

            @Override
            public void onEvent(DomainEvent event) {
                // No-op observer used only to validate bean wiring in this test.
            }
        };

        assertThat(applicationConfiguration.clock()).isNotNull();
        assertThat(applicationConfiguration.complaintCategoryClassifier()).isInstanceOf(ComplaintCategoryClassifier.class);
        assertThat(applicationConfiguration.complaintSlaPolicy()).isInstanceOf(ComplaintSlaPolicy.class);
        assertThat(resilientExecutor).isNotNull();
        assertThat(resilientExecutor.isCallPermitted(com.complaintmanagementservice.infrastructure.resilience.ResilienceProfile.PERSISTENCE)).isTrue();

        DomainEventPublisher domainEventPublisher = applicationConfiguration.domainEventPublisher(List.of(observer));
        CreateComplaintUseCase createComplaintUseCase = applicationConfiguration.createComplaintUseCase(
                mock(CategoryCatalogPort.class),
                mock(ComplaintRepositoryPort.class),
                domainEventPublisher,
                new ComplaintCategoryClassifier(),
                applicationConfiguration.clock()
        );
        SearchComplaintsUseCase searchComplaintsUseCase = applicationConfiguration.searchComplaintsUseCase(mock(ComplaintRepositoryPort.class));
        PublishSlaWarningsUseCase publishSlaWarningsUseCase = applicationConfiguration.publishSlaWarningsUseCase(
                mock(ComplaintRepositoryPort.class),
                mock(ComplaintSlaWarningMessagePort.class),
                new ComplaintSlaPolicy(),
                applicationConfiguration.clock()
        );

        assertThat(domainEventPublisher).isNotNull();
        assertThat(createComplaintUseCase).isNotNull();
        assertThat(searchComplaintsUseCase).isNotNull();
        assertThat(publishSlaWarningsUseCase).isNotNull();
    }

    @Test
    void shouldCreateMessagingInfrastructure() throws Exception {
        MessagingProperties properties = new MessagingProperties(
                "test-broker",
                new MessagingProperties.QueueNames("received", "created", "warning"),
                new MessagingProperties.RedeliverySettings(3, 10, 2.0)
        );
        BrokerService brokerService = messagingConfiguration.embeddedBroker(properties);
        ActiveMQConnectionFactory connectionFactory =
                (ActiveMQConnectionFactory) messagingConfiguration.connectionFactory(properties);
        MessageConverter messageConverter = messagingConfiguration.messageConverter(new com.fasterxml.jackson.databind.ObjectMapper());
        DefaultJmsListenerContainerFactory factory =
                messagingConfiguration.complaintListenerContainerFactory(connectionFactory, messageConverter);
        ErrorHandler errorHandler = (ErrorHandler) ReflectionTestUtils.getField(factory, "errorHandler");

        assertThat(brokerService.isPersistent()).isFalse();
        assertThat(connectionFactory).isNotNull();
        assertThat(connectionFactory.isTrustAllPackages()).isFalse();
        assertThat(connectionFactory.getTrustedPackages()).containsExactly(
                "com.complaintmanagementservice.adapters.in.messaging.dto",
                "com.complaintmanagementservice.adapters.out.messaging.dto"
        );
        assertThat(messageConverter).isInstanceOf(JacksonTextMessageConverter.class);
        assertThat(factory).isNotNull();
        assertThat(errorHandler).isNotNull();

        RuntimeException boom = new RuntimeException("boom");
        assertThatThrownBy(() -> errorHandler.handleError(boom))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Falha ao consumir a mensagem da fila de reclamações.")
                .cause()
                .isSameAs(boom);

        brokerService.start();
        brokerService.stop();
    }

    @Test
    void shouldSerializeAndDeserializeJmsMessages() throws Exception {
        JacksonTextMessageConverter converter = new JacksonTextMessageConverter(
                new com.fasterxml.jackson.databind.ObjectMapper()
                        .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
        );
        Session session = mock(Session.class);
        TextMessage textMessage = mock(TextMessage.class);
        when(session.createTextMessage(any())).thenReturn(textMessage);
        when(textMessage.getStringProperty("_type"))
                .thenReturn(com.complaintmanagementservice.adapters.out.messaging.dto.ComplaintSlaWarningQueueMessage.class.getName());
        when(textMessage.getText()).thenReturn("""
                {"complaintId":"11111111-1111-1111-1111-111111111111","slaDeadlineDate":"2026-03-30"}
                """);

        com.complaintmanagementservice.adapters.out.messaging.dto.ComplaintSlaWarningQueueMessage outboundMessage =
                new com.complaintmanagementservice.adapters.out.messaging.dto.ComplaintSlaWarningQueueMessage(
                        UUID.fromString("11111111-1111-1111-1111-111111111111"),
                        LocalDate.of(2026, 3, 30)
                );

        converter.toMessage(outboundMessage, session);
        Object message = converter.fromMessage(textMessage);

        verify(textMessage).setStringProperty("_type", outboundMessage.getClass().getName());
        assertThat(message).isInstanceOf(com.complaintmanagementservice.adapters.out.messaging.dto.ComplaintSlaWarningQueueMessage.class);
        assertThat(((com.complaintmanagementservice.adapters.out.messaging.dto.ComplaintSlaWarningQueueMessage) message).slaDeadlineDate())
                .isEqualTo(LocalDate.of(2026, 3, 30));
    }
}
