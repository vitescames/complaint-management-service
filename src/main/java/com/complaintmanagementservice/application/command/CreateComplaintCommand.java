package com.complaintmanagementservice.application.command;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class CreateComplaintCommand {

    private final String customerCpf;
    private final String customerName;
    private final LocalDate customerBirthDate;
    private final String customerEmail;
    private final LocalDate complaintCreatedDate;
    private final String complaintText;
    private final List<String> documentUrls;

    private CreateComplaintCommand(Builder builder) {
        this.customerCpf = builder.customerCpf;
        this.customerName = builder.customerName;
        this.customerBirthDate = builder.customerBirthDate;
        this.customerEmail = builder.customerEmail;
        this.complaintCreatedDate = builder.complaintCreatedDate;
        this.complaintText = builder.complaintText;
        this.documentUrls = List.copyOf(builder.documentUrls);
    }

    public static Builder builder() {
        return new Builder();
    }

    public String customerCpf() {
        return customerCpf;
    }

    public String customerName() {
        return customerName;
    }

    public LocalDate customerBirthDate() {
        return customerBirthDate;
    }

    public String customerEmail() {
        return customerEmail;
    }

    public LocalDate complaintCreatedDate() {
        return complaintCreatedDate;
    }

    public String complaintText() {
        return complaintText;
    }

    public List<String> documentUrls() {
        return documentUrls;
    }

    public static final class Builder {

        private String customerCpf;
        private String customerName;
        private LocalDate customerBirthDate;
        private String customerEmail;
        private LocalDate complaintCreatedDate;
        private String complaintText;
        private List<String> documentUrls = List.of();

        private Builder() {
        }

        public Builder customerCpf(String customerCpf) {
            this.customerCpf = customerCpf;
            return this;
        }

        public Builder customerName(String customerName) {
            this.customerName = customerName;
            return this;
        }

        public Builder customerBirthDate(LocalDate customerBirthDate) {
            this.customerBirthDate = customerBirthDate;
            return this;
        }

        public Builder customerEmail(String customerEmail) {
            this.customerEmail = customerEmail;
            return this;
        }

        public Builder complaintCreatedDate(LocalDate complaintCreatedDate) {
            this.complaintCreatedDate = complaintCreatedDate;
            return this;
        }

        public Builder complaintText(String complaintText) {
            this.complaintText = complaintText;
            return this;
        }

        public Builder documentUrls(List<String> documentUrls) {
            this.documentUrls = copyDocumentUrls(documentUrls);
            return this;
        }

        public CreateComplaintCommand build() {
            validateRequiredFields();
            return new CreateComplaintCommand(this);
        }

        private void validateRequiredFields() {
            requireText(customerCpf, "O CPF do cliente é obrigatório.");
            requireText(customerName, "O nome do cliente é obrigatório.");
            requireValue(customerBirthDate, "A data de nascimento do cliente é obrigatória.");
            requireText(customerEmail, "O e-mail do cliente é obrigatório.");
            requireValue(complaintCreatedDate, "A data da reclamação é obrigatória.");
            requireText(complaintText, "O texto da reclamação é obrigatório.");
        }

        private List<String> copyDocumentUrls(List<String> documentUrls) {
            if (documentUrls == null) {
                return List.of();
            }

            List<String> copiedDocumentUrls = new ArrayList<>();
            for (String documentUrl : documentUrls) {
                if (documentUrl != null) {
                    copiedDocumentUrls.add(documentUrl);
                }
            }
            return copiedDocumentUrls;
        }

        private void requireText(String value, String message) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException(message);
            }
        }

        private void requireValue(Object value, String message) {
            if (value == null) {
                throw new IllegalArgumentException(message);
            }
        }
    }
}
