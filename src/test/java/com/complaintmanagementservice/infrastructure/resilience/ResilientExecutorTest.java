package com.complaintmanagementservice.infrastructure.resilience;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResilientExecutorTest {

    @Test
    void shouldRetryAndSucceed() {
        Retry retry = Retry.of("retry", RetryConfig.custom().maxAttempts(2).waitDuration(Duration.ZERO).build());
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("cb");
        ResilientExecutor executor = new ResilientExecutor(
                Map.of(ResilienceProfile.PERSISTENCE, circuitBreaker),
                Map.of(ResilienceProfile.PERSISTENCE, retry)
        );
        AtomicInteger attempts = new AtomicInteger();

        String result = executor.executeSupplier(ResilienceProfile.PERSISTENCE, () -> {
            if (attempts.getAndIncrement() == 0) {
                throw new IllegalStateException("first failure");
            }
            return "ok";
        });

        assertThat(result).isEqualTo("ok");
        assertThat(attempts.get()).isEqualTo(2);
        executor.executeRunnable(ResilienceProfile.PERSISTENCE, attempts::incrementAndGet);
        assertThat(executor.isCallPermitted(ResilienceProfile.PERSISTENCE)).isTrue();
    }

    @Test
    void shouldFailFastWhenCircuitBreakerIsOpen() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowSize(1)
                .minimumNumberOfCalls(1)
                .failureRateThreshold(1f)
                .waitDurationInOpenState(Duration.ofMinutes(1))
                .build();
        CircuitBreaker circuitBreaker = CircuitBreaker.of("cb-open", config);
        Retry retry = Retry.of("retry-open", RetryConfig.custom()
                .maxAttempts(2)
                .waitDuration(Duration.ZERO)
                .ignoreExceptions(CallNotPermittedException.class)
                .build());
        ResilientExecutor executor = new ResilientExecutor(
                Map.of(ResilienceProfile.MESSAGING, circuitBreaker),
                Map.of(ResilienceProfile.MESSAGING, retry)
        );

        assertThatThrownBy(() -> executor.executeSupplier(ResilienceProfile.MESSAGING, () -> {
            throw new IllegalStateException("boom");
        })).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> executor.executeSupplier(ResilienceProfile.MESSAGING, () -> "never"))
                .isInstanceOf(CallNotPermittedException.class);
        assertThat(executor.isCallPermitted(ResilienceProfile.MESSAGING)).isFalse();
    }
}
