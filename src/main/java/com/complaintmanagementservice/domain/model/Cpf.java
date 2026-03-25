package com.complaintmanagementservice.domain.model;

import com.complaintmanagementservice.domain.exception.DomainValidationException;

public record Cpf(String value) {

    public Cpf {
        if (value == null) {
            throw new DomainValidationException("O CPF e obrigatorio");
        }
        String normalized = value.replaceAll("\\D", "");
        validate(normalized);
        value = normalized;
    }

    private static void validate(String cpf) {
        if (cpf.length() != 11 || cpf.chars().distinct().count() == 1) {
            throw new DomainValidationException("CPF invalido");
        }
        if (calculateDigit(cpf, 9) != Character.getNumericValue(cpf.charAt(9))
                || calculateDigit(cpf, 10) != Character.getNumericValue(cpf.charAt(10))) {
            throw new DomainValidationException("CPF invalido");
        }
    }

    private static int calculateDigit(String cpf, int length) {
        int sum = 0;
        for (int index = 0; index < length; index++) {
            sum += Character.getNumericValue(cpf.charAt(index)) * ((length + 1) - index);
        }
        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }
}
