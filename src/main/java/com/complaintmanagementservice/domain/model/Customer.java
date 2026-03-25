package com.complaintmanagementservice.domain.model;

import com.complaintmanagementservice.domain.exception.DomainValidationException;

import java.time.LocalDate;
import java.util.Objects;

public final class Customer {

    private final Cpf cpf;
    private final CustomerName name;
    private final LocalDate birthDate;
    private final EmailAddress emailAddress;

    public Customer(Cpf cpf, CustomerName name, LocalDate birthDate, EmailAddress emailAddress) {
        this.cpf = Objects.requireNonNull(cpf, "cpf must not be null");
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.birthDate = Objects.requireNonNull(birthDate, "birthDate must not be null");
        this.emailAddress = Objects.requireNonNull(emailAddress, "emailAddress must not be null");
        if (birthDate.isAfter(LocalDate.now())) {
            throw new DomainValidationException("Customer birth date cannot be in the future");
        }
    }

    public Cpf cpf() {
        return cpf;
    }

    public CustomerName name() {
        return name;
    }

    public LocalDate birthDate() {
        return birthDate;
    }

    public EmailAddress emailAddress() {
        return emailAddress;
    }
}
