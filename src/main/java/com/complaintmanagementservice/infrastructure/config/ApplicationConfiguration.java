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
import com.complaintmanagementservice.application.usecase.CreateComplaintService;
import com.complaintmanagementservice.application.usecase.PublishSlaWarningsService;
import com.complaintmanagementservice.application.usecase.SearchComplaintsService;
import com.complaintmanagementservice.domain.service.ComplaintCategoryClassifier;
import com.complaintmanagementservice.domain.service.ComplaintSlaPolicy;
import com.complaintmanagementservice.infrastructure.event.SpringComplaintCreatedEventObserver;
import com.complaintmanagementservice.infrastructure.event.SpringDomainEventPublisherAdapter;
import com.complaintmanagementservice.infrastructure.resilience.ResilienceProfile;
import com.complaintmanagementservice.infrastructure.resilience.ResilientExecutor;
import com.complaintmanagementservice.infrastructure.scheduler.SlaWarningScheduler;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.core.JmsTemplate;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;

@Configuration
public class ApplicationConfiguration {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public ComplaintCategoryClassifier complaintCategoryClassifier() {
        return new ComplaintCategoryClassifier();
    }

    @Bean
    public ComplaintSlaPolicy complaintSlaPolicy() {
        return new ComplaintSlaPolicy();
    }

    @Bean
    public CreateComplaintRestRequestMapper createComplaintRestRequestMapper() {
        return new CreateComplaintRestRequestMapper();
    }

    @Bean
    public SearchComplaintsQueryMapper searchComplaintsQueryMapper() {
        return new SearchComplaintsQueryMapper();
    }

    @Bean
    public ComplaintResponseMapper complaintResponseMapper() {
        return new ComplaintResponseMapper();
    }

    @Bean
    public CreateComplaintQueueMessageMapper createComplaintQueueMessageMapper() {
        return new CreateComplaintQueueMessageMapper();
    }

    @Bean
    public ComplaintMessagePayloadMapper complaintMessagePayloadMapper() {
        return new ComplaintMessagePayloadMapper();
    }

    @Bean
    public CategoryPersistenceMapper categoryPersistenceMapper() {
        return new CategoryPersistenceMapper();
    }

    @Bean
    public ComplaintPersistenceMapper complaintPersistenceMapper(CategoryPersistenceMapper categoryPersistenceMapper) {
        return new ComplaintPersistenceMapper(categoryPersistenceMapper);
    }

    @Bean
    public ResilientExecutor resilientExecutor(ResilienceProperties resilienceProperties) {
        return new ResilientExecutor(
                Map.of(
                        ResilienceProfile.PERSISTENCE, circuitBreaker("persistence", resilienceProperties.profiles().persistence()),
                        ResilienceProfile.MESSAGING, circuitBreaker("messaging", resilienceProperties.profiles().messaging())
                ),
                Map.of(
                        ResilienceProfile.PERSISTENCE, retry("persistence", resilienceProperties.profiles().persistence()),
                        ResilienceProfile.MESSAGING, retry("messaging", resilienceProperties.profiles().messaging())
                )
        );
    }

    @Bean
    public ComplaintRepositoryPort complaintRepositoryPort(
            ComplaintJpaRepository complaintJpaRepository,
            CustomerJpaRepository customerJpaRepository,
            ComplaintStatusJpaRepository complaintStatusJpaRepository,
            CategoryJpaRepository categoryJpaRepository,
            ComplaintPersistenceMapper complaintPersistenceMapper,
            ResilientExecutor resilientExecutor
    ) {
        return new ComplaintPersistenceAdapter(
                complaintJpaRepository,
                customerJpaRepository,
                complaintStatusJpaRepository,
                categoryJpaRepository,
                complaintPersistenceMapper,
                resilientExecutor
        );
    }

    @Bean
    public CategoryCatalogPort categoryCatalogPort(
            CategoryJpaRepository categoryJpaRepository,
            CategoryPersistenceMapper categoryPersistenceMapper,
            ResilientExecutor resilientExecutor
    ) {
        return new CategoryCatalogPersistenceAdapter(categoryJpaRepository, categoryPersistenceMapper, resilientExecutor);
    }

    @Bean
    public ComplaintQueuePublisherAdapter complaintQueuePublisherAdapter(
            JmsTemplate jmsTemplate,
            MessagingProperties messagingProperties,
            ComplaintMessagePayloadMapper complaintMessagePayloadMapper,
            ResilientExecutor resilientExecutor
    ) {
        return new ComplaintQueuePublisherAdapter(jmsTemplate, messagingProperties, complaintMessagePayloadMapper, resilientExecutor);
    }

    @Bean
    public DomainEventPublisherPort domainEventPublisherPort(ApplicationEventPublisher applicationEventPublisher) {
        return new SpringDomainEventPublisherAdapter(applicationEventPublisher);
    }

    @Bean
    public CreateComplaintUseCase createComplaintUseCase(
            CategoryCatalogPort categoryCatalogPort,
            ComplaintRepositoryPort complaintRepositoryPort,
            DomainEventPublisherPort domainEventPublisherPort,
            ComplaintCategoryClassifier complaintCategoryClassifier,
            Clock clock
    ) {
        return new CreateComplaintService(
                categoryCatalogPort,
                complaintRepositoryPort,
                domainEventPublisherPort,
                complaintCategoryClassifier,
                clock
        );
    }

    @Bean
    public SearchComplaintsUseCase searchComplaintsUseCase(ComplaintRepositoryPort complaintRepositoryPort) {
        return new SearchComplaintsService(complaintRepositoryPort);
    }

    @Bean
    public PublishSlaWarningsUseCase publishSlaWarningsUseCase(
            ComplaintRepositoryPort complaintRepositoryPort,
            ComplaintSlaWarningMessagePort complaintSlaWarningMessagePort,
            ComplaintSlaPolicy complaintSlaPolicy,
            Clock clock
    ) {
        return new PublishSlaWarningsService(complaintRepositoryPort, complaintSlaWarningMessagePort, complaintSlaPolicy, clock);
    }

    @Bean
    public ComplaintCreatedDomainEventHandler complaintCreatedDomainEventHandler(ComplaintCreatedMessagePort complaintCreatedMessagePort) {
        return new ComplaintCreatedDomainEventHandler(complaintCreatedMessagePort);
    }

    @Bean
    public SpringComplaintCreatedEventObserver springComplaintCreatedEventObserver(
            ComplaintCreatedDomainEventHandler complaintCreatedDomainEventHandler
    ) {
        return new SpringComplaintCreatedEventObserver(complaintCreatedDomainEventHandler);
    }

    @Bean
    public SlaWarningScheduler slaWarningScheduler(PublishSlaWarningsUseCase publishSlaWarningsUseCase) {
        return new SlaWarningScheduler(publishSlaWarningsUseCase);
    }

    @Bean
    public ComplaintReceivedListener complaintReceivedListener(
            CreateComplaintUseCase createComplaintUseCase,
            CreateComplaintQueueMessageMapper createComplaintQueueMessageMapper,
            jakarta.validation.Validator validator
    ) {
        return new ComplaintReceivedListener(createComplaintUseCase, createComplaintQueueMessageMapper, validator);
    }

    @Bean
    public ComplaintCreatedMessagePort complaintCreatedMessagePort(ComplaintQueuePublisherAdapter complaintQueuePublisherAdapter) {
        return complaintQueuePublisherAdapter;
    }

    @Bean
    public ComplaintSlaWarningMessagePort complaintSlaWarningMessagePort(ComplaintQueuePublisherAdapter complaintQueuePublisherAdapter) {
        return complaintQueuePublisherAdapter;
    }

    private CircuitBreaker circuitBreaker(String name, ResilienceProperties.ProfileSettings settings) {
        return CircuitBreaker.of(name, CircuitBreakerConfig.custom()
                .failureRateThreshold(settings.failureRateThreshold())
                .slidingWindowSize(settings.slidingWindowSize())
                .minimumNumberOfCalls(settings.minimumNumberOfCalls())
                .waitDurationInOpenState(Duration.ofMillis(settings.waitDurationInOpenStateMillis()))
                .build());
    }

    private Retry retry(String name, ResilienceProperties.ProfileSettings settings) {
        return Retry.of(name, RetryConfig.custom()
                .maxAttempts(settings.maxAttempts())
                .waitDuration(Duration.ofMillis(settings.waitDurationMillis()))
                .ignoreExceptions(io.github.resilience4j.circuitbreaker.CallNotPermittedException.class)
                .build());
    }
}
