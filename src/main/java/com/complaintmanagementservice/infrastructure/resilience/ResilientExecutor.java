package com.complaintmanagementservice.infrastructure.resilience;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;

import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

public class ResilientExecutor {

    private final Map<ResilienceProfile, CircuitBreaker> circuitBreakers;
    private final Map<ResilienceProfile, Retry> retries;

    public ResilientExecutor(Map<ResilienceProfile, CircuitBreaker> circuitBreakers, Map<ResilienceProfile, Retry> retries) {
        this.circuitBreakers = Map.copyOf(Objects.requireNonNull(circuitBreakers, "circuitBreakers must not be null"));
        this.retries = Map.copyOf(Objects.requireNonNull(retries, "retries must not be null"));
    }

    public <T> T executeSupplier(ResilienceProfile profile, Supplier<T> supplier) {
        Retry retry = retries.get(profile);
        CircuitBreaker circuitBreaker = circuitBreakers.get(profile);
        Supplier<T> retryableSupplier = Retry.decorateSupplier(retry, supplier);
        Supplier<T> protectedSupplier = CircuitBreaker.decorateSupplier(circuitBreaker, retryableSupplier);
        return protectedSupplier.get();
    }

    public void executeRunnable(ResilienceProfile profile, Runnable runnable) {
        executeSupplier(profile, () -> {
            runnable.run();
            return null;
        });
    }

    public boolean isCallPermitted(ResilienceProfile profile) {
        try {
            executeRunnable(profile, () -> {
            });
            return true;
        }
        catch (CallNotPermittedException exception) {
            return false;
        }
    }
}
