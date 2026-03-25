package com.complaintmanagementservice.domain.model;

import com.complaintmanagementservice.TestFixtures;
import com.complaintmanagementservice.domain.event.ComplaintCreatedDomainEvent;
import com.complaintmanagementservice.domain.exception.DomainValidationException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
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
        Category category = Category.builder().id(4L).name("acesso").keywords(Set.of(keyword)).build();
        Category categoryWithoutKeywords = Category.builder().id(5L).name("fraude").build();

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
        assertThat(categoryWithoutKeywords.keywords()).isEmpty();
        assertThat(ComplaintStatus.fromId(1)).isEqualTo(ComplaintStatus.PENDING);
        assertThat(ComplaintStatus.PROCESSING.id()).isEqualTo(2);
    }

    @Test
    void shouldRejectInvalidValueObjectsAndEntities() {
        assertThatThrownBy(() -> new ComplaintId(null))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("O identificador da reclamacao e obrigatorio");
        assertThatThrownBy(() -> ComplaintId.from("invalid"))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("O identificador da reclamacao e invalido");
        assertThatThrownBy(() -> new Cpf(null))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("O CPF e obrigatorio");
        assertThatThrownBy(() -> new Cpf("11111111111"))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("CPF invalido");
        assertThatThrownBy(() -> new Cpf("123"))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("CPF invalido");
        assertThatThrownBy(() -> new Cpf("52998224735"))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("CPF invalido");
        assertThatThrownBy(() -> new Cpf("52998224724"))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("CPF invalido");
        assertThatThrownBy(() -> new EmailAddress(null))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("O e-mail e obrigatorio");
        assertThatThrownBy(() -> new EmailAddress("invalid"))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("E-mail invalido");
        assertThatThrownBy(() -> new CustomerName(null))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("O nome do cliente e obrigatorio");
        assertThatThrownBy(() -> new CustomerName(" "))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("O nome do cliente deve ter entre 1 e 120 caracteres");
        assertThatThrownBy(() -> new CustomerName("a".repeat(121)))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("O nome do cliente deve ter entre 1 e 120 caracteres");
        assertThatThrownBy(() -> new ComplaintText(null))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("O texto da reclamacao e obrigatorio");
        assertThatThrownBy(() -> new ComplaintText(" "))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("O texto da reclamacao deve ter entre 1 e 4000 caracteres");
        assertThatThrownBy(() -> new ComplaintText("a".repeat(4001)))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("O texto da reclamacao deve ter entre 1 e 4000 caracteres");
        assertThatThrownBy(() -> new DocumentUrl(null))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("A URL do documento e obrigatoria");
        assertThatThrownBy(() -> new DocumentUrl("ftp://example.com"))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("A URL do documento deve ser HTTP ou HTTPS absoluta");
        assertThatThrownBy(() -> new DocumentUrl("relative/path"))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("A URL do documento deve ser HTTP ou HTTPS absoluta");
        assertThatThrownBy(() -> new DocumentUrl("://broken"))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("A URL do documento deve ser HTTP ou HTTPS absoluta");
        assertThatThrownBy(() -> ComplaintStatus.fromId(99))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("O status da reclamacao e invalido");
        assertThatThrownBy(() -> new CategoryKeyword(null, "login"))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("O identificador da palavra-chave da categoria e obrigatorio");
        assertThatThrownBy(() -> new CategoryKeyword(1L, null))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("A palavra-chave da categoria e obrigatoria");
        assertThatThrownBy(() -> new CategoryKeyword(1L, " "))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("A palavra-chave da categoria e obrigatoria");
        assertThatThrownBy(() -> Category.builder().name("acesso").build())
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("O identificador da categoria e obrigatorio");
        assertThatThrownBy(() -> Category.builder().id(1L).build())
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("O nome da categoria e obrigatorio");
        assertThatThrownBy(() -> Category.builder().id(1L).name(" ").keywords(Set.of()).build())
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("O nome da categoria e obrigatorio");
        assertThatThrownBy(() -> Customer.builder()
                .name(new CustomerName("Maria"))
                .birthDate(LocalDate.of(1990, 6, 15))
                .emailAddress(new EmailAddress("maria@example.com"))
                .build())
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("O CPF do cliente e obrigatorio");
        assertThatThrownBy(() -> Customer.builder()
                .cpf(new Cpf("52998224725"))
                .birthDate(LocalDate.of(1990, 6, 15))
                .emailAddress(new EmailAddress("maria@example.com"))
                .build())
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("O nome do cliente e obrigatorio");
        assertThatThrownBy(() -> Customer.builder()
                .cpf(new Cpf("52998224725"))
                .name(new CustomerName("Maria"))
                .emailAddress(new EmailAddress("maria@example.com"))
                .build())
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("A data de nascimento do cliente e obrigatoria");
        assertThatThrownBy(() -> Customer.builder()
                .cpf(new Cpf("52998224725"))
                .name(new CustomerName("Maria"))
                .birthDate(LocalDate.of(1990, 6, 15))
                .build())
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("O e-mail do cliente e obrigatorio");
        assertThatThrownBy(() -> Customer.builder()
                .cpf(new Cpf("52998224725"))
                .name(new CustomerName("Maria"))
                .birthDate(LocalDate.now().plusDays(1))
                .emailAddress(new EmailAddress("maria@example.com"))
                .build())
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("A data de nascimento do cliente nao pode ser futura");
    }

    @Test
    void shouldCreateAndReconstituteComplaint() {
        Complaint createdComplaint = Complaint.builder()
                .customer(TestFixtures.customer())
                .complaintDate(LocalDate.of(2026, 3, 20))
                .complaintText(new ComplaintText("texto"))
                .documentUrls(List.of(new DocumentUrl("https://example.com/doc")))
                .categories(Set.of(TestFixtures.acessoCategory()))
                .clock(TestFixtures.FIXED_CLOCK)
                .buildNew();

        assertThat(createdComplaint.status()).isEqualTo(ComplaintStatus.PENDING);
        assertThat(createdComplaint.documentUrls()).hasSize(1);
        assertThat(createdComplaint.categories()).extracting(Category::name).containsExactly("acesso");
        assertThat(createdComplaint.registeredAt()).isEqualTo(TestFixtures.FIXED_CLOCK.instant());

        List<?> domainEvents = createdComplaint.pullDomainEvents();
        assertThat(domainEvents).hasSize(1);
        assertThat(domainEvents.get(0)).isInstanceOf(ComplaintCreatedDomainEvent.class);
        assertThat(createdComplaint.pullDomainEvents()).isEmpty();

        Complaint restoredComplaint = Complaint.reconstitute(
                new ComplaintId(UUID.fromString("11111111-1111-1111-1111-111111111111")),
                TestFixtures.customer(),
                LocalDate.of(2026, 3, 20),
                new ComplaintText("texto"),
                null,
                ComplaintStatus.RESOLVED,
                null,
                TestFixtures.FIXED_CLOCK.instant()
        );

        assertThat(restoredComplaint.id().value()).isEqualTo(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        assertThat(restoredComplaint.status()).isEqualTo(ComplaintStatus.RESOLVED);
        assertThat(restoredComplaint.documentUrls()).isEmpty();
        assertThat(restoredComplaint.categories()).isEmpty();
        assertThat(restoredComplaint.pullDomainEvents()).isEmpty();
    }

    @Test
    void shouldRejectIncompleteComplaintConstruction() {
        assertThatThrownBy(() -> Complaint.builder()
                .customer(TestFixtures.customer())
                .complaintDate(LocalDate.of(2026, 3, 20))
                .complaintText(new ComplaintText("texto"))
                .categories(Set.of())
                .buildNew())
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("O relogio de criacao da reclamacao e obrigatorio");
        assertThatThrownBy(() -> Complaint.builder()
                .customer(TestFixtures.customer())
                .complaintDate(LocalDate.of(2026, 3, 20))
                .complaintText(new ComplaintText("texto"))
                .status(ComplaintStatus.PENDING)
                .registeredAt(Instant.now())
                .buildReconstituted())
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("O identificador da reclamacao e obrigatorio");
        assertThatThrownBy(() -> Complaint.builder()
                .id(ComplaintId.newId())
                .complaintDate(LocalDate.of(2026, 3, 20))
                .complaintText(new ComplaintText("texto"))
                .status(ComplaintStatus.PENDING)
                .registeredAt(Instant.now())
                .buildReconstituted())
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("O cliente da reclamacao e obrigatorio");
        assertThatThrownBy(() -> Complaint.builder()
                .id(ComplaintId.newId())
                .customer(TestFixtures.customer())
                .complaintText(new ComplaintText("texto"))
                .status(ComplaintStatus.PENDING)
                .registeredAt(Instant.now())
                .buildReconstituted())
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("A data da reclamacao e obrigatoria");
        assertThatThrownBy(() -> Complaint.builder()
                .id(ComplaintId.newId())
                .customer(TestFixtures.customer())
                .complaintDate(LocalDate.of(2026, 3, 20))
                .status(ComplaintStatus.PENDING)
                .registeredAt(Instant.now())
                .buildReconstituted())
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("O texto da reclamacao e obrigatorio");
        assertThatThrownBy(() -> Complaint.builder()
                .id(ComplaintId.newId())
                .customer(TestFixtures.customer())
                .complaintDate(LocalDate.of(2026, 3, 20))
                .complaintText(new ComplaintText("texto"))
                .registeredAt(Instant.now())
                .buildReconstituted())
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("O status da reclamacao e obrigatorio");
        assertThatThrownBy(() -> Complaint.builder()
                .id(ComplaintId.newId())
                .customer(TestFixtures.customer())
                .complaintDate(LocalDate.of(2026, 3, 20))
                .complaintText(new ComplaintText("texto"))
                .status(ComplaintStatus.PENDING)
                .buildReconstituted())
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("A data de registro da reclamacao e obrigatoria");
    }

    @Test
    void shouldValidateComplaintCreatedDomainEvent() {
        ComplaintCreatedDomainEvent event = new ComplaintCreatedDomainEvent(TestFixtures.complaint().id(), TestFixtures.FIXED_CLOCK.instant());

        assertThat(event.complaintId()).isEqualTo(TestFixtures.complaint().id());
        assertThat(event.occurredAt()).isEqualTo(TestFixtures.FIXED_CLOCK.instant());
        assertThatThrownBy(() -> new ComplaintCreatedDomainEvent(null, Instant.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("complaintId must not be null");
        assertThatThrownBy(() -> new ComplaintCreatedDomainEvent(TestFixtures.complaint().id(), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("occurredAt must not be null");
    }
}
