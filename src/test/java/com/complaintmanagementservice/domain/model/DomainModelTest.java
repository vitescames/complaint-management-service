package com.complaintmanagementservice.domain.model;

import com.complaintmanagementservice.TestFixtures;
import com.complaintmanagementservice.domain.event.ComplaintCreatedDomainEvent;
import com.complaintmanagementservice.domain.exception.DomainValidationException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DomainModelTest {

    @Test
    void shouldValidateValueObjectsAndEntities() {
        ComplaintId complaintId = ComplaintId.from("11111111-1111-1111-1111-111111111111");
        Cpf cpf = new Cpf("529.982.247-25");
        Cpf cpfWithZeroDigit = new Cpf("10000000108");
        EmailAddress emailAddress = new EmailAddress(" USER@Example.com ");
        CustomerName customerName = new CustomerName(" Maria ");
        ComplaintText complaintText = new ComplaintText(" Texto valido ");
        DocumentUrl httpDocumentUrl = new DocumentUrl("http://example.com/file");
        DocumentUrl documentUrl = new DocumentUrl("https://example.com/file");
        Customer customer = TestFixtures.customer();
        CategoryKeyword keyword = new CategoryKeyword(1L, " login ");
        Category category = new Category(4L, "acesso", Set.of(keyword));

        assertThat(complaintId.toString()).isEqualTo("11111111-1111-1111-1111-111111111111");
        assertThat(cpf.value()).isEqualTo("52998224725");
        assertThat(cpfWithZeroDigit.value()).isEqualTo("10000000108");
        assertThat(emailAddress.value()).isEqualTo("user@example.com");
        assertThat(customerName.value()).isEqualTo("Maria");
        assertThat(complaintText.value()).isEqualTo("Texto valido");
        assertThat(httpDocumentUrl.value()).isEqualTo("http://example.com/file");
        assertThat(documentUrl.value()).isEqualTo("https://example.com/file");
        assertThat(customer.cpf().value()).isEqualTo("52998224725");
        assertThat(keyword.id()).isEqualTo(1L);
        assertThat(keyword.value()).isEqualTo("login");
        assertThat(keyword.matches("erro de login")).isTrue();
        assertThat(keyword.matches("sem relacao")).isFalse();
        assertThat(category.id()).isEqualTo(4L);
        assertThat(category.name()).isEqualTo("acesso");
        assertThat(category.keywords()).containsExactly(keyword);
        assertThat(ComplaintStatus.fromId(1)).isEqualTo(ComplaintStatus.PENDING);
        assertThat(ComplaintStatus.PROCESSING.id()).isEqualTo(2);
    }

    @Test
    void shouldRejectInvalidDomainInputs() {
        assertThatThrownBy(() -> ComplaintId.from("invalid"))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("Complaint id must be a valid UUID");
        assertThatThrownBy(() -> new Cpf("11111111111"))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("CPF must be valid");
        assertThatThrownBy(() -> new Cpf("123"))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("CPF must be valid");
        assertThatThrownBy(() -> new Cpf("52998224735"))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("CPF must be valid");
        assertThatThrownBy(() -> new Cpf("52998224724"))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("CPF must be valid");
        assertThatThrownBy(() -> new EmailAddress("invalid"))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("Email address must be valid");
        assertThatThrownBy(() -> new CustomerName(" "))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("Customer name must contain between 1 and 120 characters");
        assertThatThrownBy(() -> new CustomerName("a".repeat(121)))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("Customer name must contain between 1 and 120 characters");
        assertThatThrownBy(() -> new ComplaintText(" "))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("Complaint text must contain between 1 and 4000 characters");
        assertThatThrownBy(() -> new ComplaintText("a".repeat(4001)))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("Complaint text must contain between 1 and 4000 characters");
        assertThatThrownBy(() -> new DocumentUrl("ftp://example.com"))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("Document URL must be an absolute HTTP or HTTPS URL");
        assertThatThrownBy(() -> new DocumentUrl("relative/path"))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("Document URL must be an absolute HTTP or HTTPS URL");
        assertThatThrownBy(() -> new DocumentUrl("://broken"))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("Document URL must be an absolute HTTP or HTTPS URL");
        assertThatThrownBy(() -> ComplaintStatus.fromId(99))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("Complaint status id must be valid");
        assertThatThrownBy(() -> new Customer(
                new Cpf("52998224725"),
                new CustomerName("Maria"),
                LocalDate.now().plusDays(1),
                new EmailAddress("maria@example.com")
        )).isInstanceOf(DomainValidationException.class)
                .hasMessage("Customer birth date cannot be in the future");
        assertThatThrownBy(() -> new CategoryKeyword(1L, " "))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("Category keyword must not be blank");
        assertThatThrownBy(() -> new Category(1L, " ", Set.of()))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("Category name must not be blank");
    }

    @Test
    void shouldCreateAndRestoreComplaint() {
        Complaint createdComplaint = Complaint.create(
                TestFixtures.customer(),
                LocalDate.of(2026, 3, 20),
                new ComplaintText("texto"),
                List.of(new DocumentUrl("https://example.com/doc")),
                Set.of(TestFixtures.acessoCategory()),
                TestFixtures.FIXED_CLOCK
        );

        assertThat(createdComplaint.status()).isEqualTo(ComplaintStatus.PENDING);
        assertThat(createdComplaint.documentUrls()).hasSize(1);
        assertThat(createdComplaint.categories()).extracting(Category::name).containsExactly("acesso");
        assertThat(createdComplaint.registeredAt()).isEqualTo(TestFixtures.FIXED_CLOCK.instant());

        List<?> domainEvents = createdComplaint.pullDomainEvents();
        assertThat(domainEvents).hasSize(1);
        assertThat(domainEvents.get(0)).isInstanceOf(ComplaintCreatedDomainEvent.class);
        assertThat(createdComplaint.pullDomainEvents()).isEmpty();

        Complaint restoredComplaint = Complaint.restore(
                new ComplaintId(UUID.fromString("11111111-1111-1111-1111-111111111111")),
                TestFixtures.customer(),
                LocalDate.of(2026, 3, 20),
                new ComplaintText("texto"),
                List.of(),
                ComplaintStatus.RESOLVED,
                Set.of(),
                TestFixtures.FIXED_CLOCK.instant()
        );

        assertThat(restoredComplaint.id().value()).isEqualTo(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        assertThat(restoredComplaint.status()).isEqualTo(ComplaintStatus.RESOLVED);
        assertThat(restoredComplaint.pullDomainEvents()).isEmpty();
    }
}
