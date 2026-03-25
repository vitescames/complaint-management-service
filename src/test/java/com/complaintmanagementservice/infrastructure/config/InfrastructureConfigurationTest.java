package com.complaintmanagementservice.infrastructure.config;

import com.complaintmanagementservice.adapters.in.messaging.ComplaintReceivedListener;
import com.complaintmanagementservice.adapters.in.messaging.mapper.CreateComplaintQueueMessageMapper;
import com.complaintmanagementservice.adapters.in.rest.mapper.ComplaintResponseMapper;
import com.complaintmanagementservice.adapters.in.rest.mapper.CreateComplaintRestRequestMapper;
import com.complaintmanagementservice.adapters.in.rest.mapper.SearchComplaintsQueryMapper;
import com.complaintmanagementservice.adapters.out.messaging.ComplaintMessagePayloadMapper;
import com.complaintmanagementservice.adapters.out.messaging.ComplaintQueuePublisherAdapter;
import com.complaintmanagementservice.adapters.out.persistence.CategoryCatalogPersistenceAdapter;
import com.complaintmanagementservice.adapters.out.persistence.ComplaintPersistenceAdapter;
import com.complaintmanagementservice.adapters.out.persistence.mapper.CategoryPersistenceMapper;
import com.complaintmanagementservice.adapters.out.persistence.mapper.ComplaintPersistenceMapper;
import com.complaintmanagementservice.adapters.out.persistence.repository.CategoryJpaRepository;
import com.complaintmanagementservice.adapters.out.persistence.repository.ComplaintJpaRepository;
import com.complaintmanagementservice.adapters.out.persistence.repository.ComplaintStatusJpaRepository;
import com.complaintmanagementservice.adapters.out.persistence.repository.CustomerJpaRepository;
import com.complaintmanagementservice.application.port.in.CreateComplaintUseCase;
import com.complaintmanagementservice.application.port.in.PublishSlaWarningsUseCase;
import com.complaintmanagementservice.application.port.in.SearchComplaintsUseCase;
import com.complaintmanagementservice.application.port.out.CategoryCatalogPort;
import com.complaintmanagementservice.application.port.out.ComplaintCreatedMessagePort;
import com.complaintmanagementservice.application.port.out.ComplaintRepositoryPort;
import com.complaintmanagementservice.application.port.out.ComplaintSlaWarningMessagePort;
import com.complaintmanagementservice.application.port.out.DomainEventPublisherPort;
import com.complaintmanagementservice.application.usecase.ComplaintCreatedDomainEventHandler;
import com.complaintmanagementservice.domain.service.ComplaintCategoryClassifier;
import com.complaintmanagementservice.domain.service.ComplaintSlaPolicy;
import com.complaintmanagementservice.infrastructure.event.SpringComplaintCreatedEventObserver;
import com.complaintmanagementservice.infrastructure.event.SpringDomainEventPublisherAdapter;
import com.complaintmanagementservice.infrastructure.scheduler.SlaWarningScheduler;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import jakarta.jms.ConnectionFactory;
import jakarta.validation.Validation;
import org.apache.activemq.broker.BrokerService;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.ErrorHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class InfrastructureConfigurationTest {

    private final ApplicationConfiguration applicationConfiguration = new ApplicationConfiguration();
    private final MessagingConfiguration messagingConfiguration = new MessagingConfiguration();

    @Test
    void shouldCreateApplicationBeans() {
        ResilienceProperties resilienceProperties = new ResilienceProperties(
                new ResilienceProperties.Profiles(
                        new ResilienceProperties.ProfileSettings(2, 0, 2, 1, 50f, 1000),
                        new ResilienceProperties.ProfileSettings(2, 0, 2, 1, 50f, 1000)
                )
        );
        var resilientExecutor = applicationConfiguration.resilientExecutor(resilienceProperties);
        assertThat(applicationConfiguration.clock()).isNotNull();
        assertThat(applicationConfiguration.complaintCategoryClassifier()).isInstanceOf(ComplaintCategoryClassifier.class);
        assertThat(applicationConfiguration.complaintSlaPolicy()).isInstanceOf(ComplaintSlaPolicy.class);
        assertThat(applicationConfiguration.createComplaintRestRequestMapper()).isInstanceOf(CreateComplaintRestRequestMapper.class);
        assertThat(applicationConfiguration.searchComplaintsQueryMapper()).isInstanceOf(SearchComplaintsQueryMapper.class);
        assertThat(applicationConfiguration.complaintResponseMapper()).isInstanceOf(ComplaintResponseMapper.class);
        assertThat(applicationConfiguration.createComplaintQueueMessageMapper()).isInstanceOf(CreateComplaintQueueMessageMapper.class);
        assertThat(applicationConfiguration.complaintMessagePayloadMapper()).isInstanceOf(ComplaintMessagePayloadMapper.class);
        CategoryPersistenceMapper categoryPersistenceMapper = applicationConfiguration.categoryPersistenceMapper();
        ComplaintPersistenceMapper complaintPersistenceMapper = applicationConfiguration.complaintPersistenceMapper(categoryPersistenceMapper);
        assertThat(categoryPersistenceMapper).isNotNull();
        assertThat(complaintPersistenceMapper).isNotNull();
        assertThat(resilientExecutor).isNotNull();
        assertThat(resilientExecutor.isCallPermitted(com.complaintmanagementservice.infrastructure.resilience.ResilienceProfile.PERSISTENCE)).isTrue();

        ComplaintQueuePublisherAdapter publisherAdapter = applicationConfiguration.complaintQueuePublisherAdapter(
                mock(org.springframework.jms.core.JmsTemplate.class),
                new MessagingProperties("broker", new MessagingProperties.QueueProperties("received", "created", "warning"),
                        new MessagingProperties.RedeliveryProperties(3, 10, 2.0)),
                new ComplaintMessagePayloadMapper(),
                resilientExecutor
        );
        assertThat(publisherAdapter).isNotNull();
        assertThat(applicationConfiguration.complaintRepositoryPort(
                mock(ComplaintJpaRepository.class),
                mock(CustomerJpaRepository.class),
                mock(ComplaintStatusJpaRepository.class),
                mock(CategoryJpaRepository.class),
                complaintPersistenceMapper,
                resilientExecutor
        )).isInstanceOf(ComplaintPersistenceAdapter.class);
        assertThat(applicationConfiguration.categoryCatalogPort(
                mock(CategoryJpaRepository.class),
                categoryPersistenceMapper,
                resilientExecutor
        )).isInstanceOf(CategoryCatalogPersistenceAdapter.class);
        DomainEventPublisherPort domainEventPublisherPort = applicationConfiguration.domainEventPublisherPort(mock(ApplicationEventPublisher.class));
        CreateComplaintUseCase createComplaintUseCase = applicationConfiguration.createComplaintUseCase(
                mock(CategoryCatalogPort.class),
                mock(ComplaintRepositoryPort.class),
                domainEventPublisherPort,
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
        ComplaintCreatedDomainEventHandler handler = applicationConfiguration.complaintCreatedDomainEventHandler(
                mock(ComplaintCreatedMessagePort.class)
        );
        SpringComplaintCreatedEventObserver observer = applicationConfiguration.springComplaintCreatedEventObserver(handler);
        ComplaintReceivedListener listener = applicationConfiguration.complaintReceivedListener(
                createComplaintUseCase,
                new CreateComplaintQueueMessageMapper(),
                Validation.buildDefaultValidatorFactory().getValidator()
        );
        SlaWarningScheduler scheduler = applicationConfiguration.slaWarningScheduler(publishSlaWarningsUseCase);

        assertThat(createComplaintUseCase).isNotNull();
        assertThat(searchComplaintsUseCase).isNotNull();
        assertThat(publishSlaWarningsUseCase).isNotNull();
        assertThat(handler).isNotNull();
        assertThat(observer).isNotNull();
        assertThat(listener).isNotNull();
        assertThat(scheduler).isNotNull();
        assertThat(applicationConfiguration.complaintCreatedMessagePort(publisherAdapter)).isSameAs(publisherAdapter);
        assertThat(applicationConfiguration.complaintSlaWarningMessagePort(publisherAdapter)).isSameAs(publisherAdapter);
    }

    @Test
    void shouldCreateMessagingInfrastructure() throws Exception {
        MessagingProperties properties = new MessagingProperties(
                "test-broker",
                new MessagingProperties.QueueProperties("received", "created", "warning"),
                new MessagingProperties.RedeliveryProperties(3, 10, 2.0)
        );
        BrokerService brokerService = messagingConfiguration.embeddedBroker(properties);
        ConnectionFactory connectionFactory = messagingConfiguration.connectionFactory(properties);
        MessageConverter messageConverter = messagingConfiguration.messageConverter();
        DefaultJmsListenerContainerFactory factory =
                messagingConfiguration.complaintListenerContainerFactory(connectionFactory, messageConverter);

        assertThat(brokerService.isPersistent()).isFalse();
        assertThat(connectionFactory).isNotNull();
        assertThat(messageConverter).isNotNull();
        assertThat(factory).isNotNull();
        ErrorHandler errorHandler = (ErrorHandler) ReflectionTestUtils.getField(factory, "errorHandler");
        assertThatThrownBy(() -> errorHandler.handleError(new RuntimeException("boom")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Complaint queue listener failed")
                .cause()
                .hasMessage("boom");
        brokerService.start();
        brokerService.stop();
    }
}
