package com.complaintmanagementservice.domain.model;

import com.complaintmanagementservice.domain.exception.DomainValidationException;

import java.time.LocalDate;

public final class Customer {

    private final Cpf cpf;
    private final CustomerName name;
    private final LocalDate birthDate;
    private final EmailAddress emailAddress;

    private Customer(Builder builder) {
        if (builder.cpf == null) {
            throw new DomainValidationException("O CPF do cliente e obrigatorio");
        }
        if (builder.name == null) {
            throw new DomainValidationException("O nome do cliente e obrigatorio");
        }
        if (builder.birthDate == null) {
            throw new DomainValidationException("A data de nascimento do cliente e obrigatoria");
        }
        if (builder.emailAddress == null) {
            throw new DomainValidationException("O e-mail do cliente e obrigatorio");
        }

        this.cpf = builder.cpf;
        this.name = builder.name;
        this.birthDate = builder.birthDate;
        this.emailAddress = builder.emailAddress;
        if (birthDate.isAfter(LocalDate.now())) {
            throw new DomainValidationException("A data de nascimento do cliente nao pode ser futura");
        }
    }

    public static Builder builder() {
        return new Builder();
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

    public static final class Builder {

        private Cpf cpf;
        private CustomerName name;
        private LocalDate birthDate;
        private EmailAddress emailAddress;

        private Builder() {
        }

        public Builder cpf(Cpf cpf) {
            this.cpf = cpf;
            return this;
        }

        public Builder name(CustomerName name) {
            this.name = name;
            return this;
        }

        public Builder birthDate(LocalDate birthDate) {
            this.birthDate = birthDate;
            return this;
        }

        public Builder emailAddress(EmailAddress emailAddress) {
            this.emailAddress = emailAddress;
            return this;
        }

        public Customer build() {
            return new Customer(this);
        }
    }
}
