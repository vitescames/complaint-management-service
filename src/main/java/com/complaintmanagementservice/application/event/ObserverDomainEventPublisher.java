package com.complaintmanagementservice.application.event;

import com.complaintmanagementservice.application.exception.RequestValidationException;
import com.complaintmanagementservice.domain.event.DomainEvent;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class ObserverDomainEventPublisher implements DomainEventPublisher {

    private final List<DomainEventObserver<? extends DomainEvent>> observers = new CopyOnWriteArrayList<>();

    @Override
    public void register(DomainEventObserver<? extends DomainEvent> observer) {
        if (observer == null) {
            throw new RequestValidationException("O observador de evento e obrigatorio");
        }
        observers.add(observer);
    }

    @Override
    public void remove(DomainEventObserver<? extends DomainEvent> observer) {
        if (observer == null) {
            throw new RequestValidationException("O observador de evento e obrigatorio");
        }
        observers.remove(observer);
    }

    @Override
    public void publish(DomainEvent event) {
        if (event == null) {
            throw new RequestValidationException("O evento de dominio e obrigatorio");
        }
        observers.stream()
                .filter(observer -> observer.supports(event))
                .forEach(observer -> notifyObserver(observer, event));
    }

    @SuppressWarnings("unchecked")
    private <T extends DomainEvent> void notifyObserver(DomainEventObserver<? extends DomainEvent> observer, DomainEvent event) {
        ((DomainEventObserver<T>) observer).onEvent((T) event);
    }
}
