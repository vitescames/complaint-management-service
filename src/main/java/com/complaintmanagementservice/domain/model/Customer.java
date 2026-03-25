package com.complaintmanagementservice.domain.model;

import com.complaintmanagementservice.domain.exception.DomainValidationException;

import java.time.LocalDate;

public final class Customer {

    private final Cpf cpf;
    private final CustomerName name;
    private final LocalDate birthDate;
    private final EmailAddress emailAddress;

    private Customer(Builder builder) {
        Cpf validatedCpf = requireCpf(builder.cpf);
        CustomerName validatedName = requireName(builder.name);
        LocalDate validatedBirthDate = requireBirthDate(builder.birthDate);
        EmailAddress validatedEmailAddress = requireEmailAddress(builder.emailAddress);

        this.cpf = validatedCpf;
        this.name = validatedName;
        this.birthDate = validatedBirthDate;
        this.emailAddress = validatedEmailAddress;
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

    private Cpf requireCpf(Cpf cpf) {
        if (cpf == null) {
            throw new DomainValidationException("O CPF do cliente é obrigatório.");
        }
        return cpf;
    }

    private CustomerName requireName(CustomerName name) {
        if (name == null) {
            throw new DomainValidationException("O nome do cliente é obrigatório.");
        }
        return name;
    }

    private LocalDate requireBirthDate(LocalDate birthDate) {
        if (birthDate == null) {
            throw new DomainValidationException("A data de nascimento do cliente é obrigatória.");
        }
        if (birthDate.isAfter(LocalDate.now())) {
            throw new DomainValidationException("A data de nascimento do cliente não pode ser futura.");
        }
        return birthDate;
    }

    private EmailAddress requireEmailAddress(EmailAddress emailAddress) {
        if (emailAddress == null) {
            throw new DomainValidationException("O e-mail do cliente é obrigatório.");
        }
        return emailAddress;
    }
}
