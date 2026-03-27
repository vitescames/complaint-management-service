package com.complaintmanagementservice.infrastructure.config;

import com.complaintmanagementservice.application.event.DomainEventObserver;
import com.complaintmanagementservice.application.event.DomainEventPublisher;
import com.complaintmanagementservice.application.event.ObserverDomainEventPublisher;
import com.complaintmanagementservice.application.port.in.CreateComplaintUseCase;
import com.complaintmanagementservice.application.port.in.PublishSlaWarningsUseCase;
import com.complaintmanagementservice.application.port.in.SearchComplaintsUseCase;
import com.complaintmanagementservice.application.port.out.CategoryCatalogPort;
import com.complaintmanagementservice.application.port.out.ComplaintRepositoryPort;
import com.complaintmanagementservice.application.usecase.CreateComplaintUseCaseImpl;
import com.complaintmanagementservice.application.usecase.PublishSlaWarningsUseCaseImpl;
import com.complaintmanagementservice.application.usecase.SearchComplaintsUseCaseImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.complaintmanagementservice.domain.event.DomainEvent;
import com.complaintmanagementservice.domain.service.ComplaintCategoryClassifier;
import com.complaintmanagementservice.domain.service.ComplaintSlaPolicy;
import com.complaintmanagementservice.infrastructure.resilience.ResilienceProfile;
import com.complaintmanagementservice.infrastructure.resilience.ResilientExecutor;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Configuration
public class ApplicationConfiguration {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper().findAndRegisterModules();
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
    public ResilientExecutor resilientExecutor(ResilienceProperties resilienceProperties) {
        return new ResilientExecutor(
                Map.of(
                        ResilienceProfile.PERSISTENCE, circuitBreaker("persistence", resilienceProperties.persistence()),
                        ResilienceProfile.MESSAGING, circuitBreaker("messaging", resilienceProperties.messaging())
                ),
                Map.of(
                        ResilienceProfile.PERSISTENCE, retry("persistence", resilienceProperties.persistence()),
                        ResilienceProfile.MESSAGING, retry("messaging", resilienceProperties.messaging())
                )
        );
    }

    @Bean
    public DomainEventPublisher domainEventPublisher(List<DomainEventObserver<? extends DomainEvent>> observers) {
        ObserverDomainEventPublisher publisher = new ObserverDomainEventPublisher();
        observers.forEach(publisher::register);
        return publisher;
    }

    @Bean
    public CreateComplaintUseCase createComplaintUseCase(
            CategoryCatalogPort categoryCatalogPort,
            ComplaintRepositoryPort complaintRepositoryPort,
            DomainEventPublisher domainEventPublisher,
            ComplaintCategoryClassifier complaintCategoryClassifier,
            Clock clock
    ) {
        return new CreateComplaintUseCaseImpl(
                categoryCatalogPort,
                complaintRepositoryPort,
                domainEventPublisher,
                complaintCategoryClassifier,
                clock
        );
    }

    @Bean
    public SearchComplaintsUseCase searchComplaintsUseCase(ComplaintRepositoryPort complaintRepositoryPort) {
        return new SearchComplaintsUseCaseImpl(complaintRepositoryPort);
    }

    @Bean
    public PublishSlaWarningsUseCase publishSlaWarningsUseCase(
            ComplaintRepositoryPort complaintRepositoryPort,
            DomainEventPublisher domainEventPublisher,
            ComplaintSlaPolicy complaintSlaPolicy,
            Clock clock
    ) {
        return new PublishSlaWarningsUseCaseImpl(complaintRepositoryPort, domainEventPublisher, complaintSlaPolicy, clock);
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
