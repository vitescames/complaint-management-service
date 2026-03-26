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
        ComplaintText complaintText = new ComplaintText(" Texto válido ");
        DocumentUrl httpDocumentUrl = new DocumentUrl("http://example.com/file");
        DocumentUrl documentUrl = new DocumentUrl("https://example.com/file");
        Customer customer = TestFixtures.customer();
        CategoryKeyword keyword = new CategoryKeyword(1L, " login ");
        Category category = Category.builder().id(4L).name("acesso").keywords(Set.of(keyword)).build();
        Category categoryWithoutKeywords = Category.builder().id(5L).name("fraude").build();

        assertThat(complaintId.value()).isEqualTo(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        assertThat(cpf.value()).isEqualTo("52998224725");
        assertThat(cpfWithZeroDigit.value()).isEqualTo("10000000108");
        assertThat(emailAddress.value()).isEqualTo("user@example.com");
        assertThat(customerName.value()).isEqualTo("Maria");
        assertThat(complaintText.value()).isEqualTo("Texto válido");
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
    void shouldRejectInvalidComplaintIdsAndCpfs() {
        assertThatThrownBy(() -> new ComplaintId(null))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("O identificador da reclamação é obrigatório.");
        assertThatThrownBy(() -> ComplaintId.from("invalid"))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("O identificador da reclamação é inválido.");
        assertThatThrownBy(() -> new Cpf(null))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("O CPF é obrigatório.");
        assertThatThrownBy(() -> new Cpf("11111111111"))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("CPF inválido.");
        assertThatThrownBy(() -> new Cpf("123"))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("CPF inválido.");
        assertThatThrownBy(() -> new Cpf("52998224735"))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("CPF inválido.");
        assertThatThrownBy(() -> new Cpf("52998224724"))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("CPF inválido.");
    }

    @Test
    void shouldRejectInvalidTextualValueObjects() {
        String oversizedCustomerName = "a".repeat(121);
        String oversizedComplaintText = "a".repeat(4001);

        assertThatThrownBy(() -> new EmailAddress(null))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("O e-mail é obrigatório.");
        assertThatThrownBy(() -> new EmailAddress("invalid"))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("E-mail inválido.");
        assertThatThrownBy(() -> new CustomerName(null))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("O nome do cliente é obrigatório.");
        assertThatThrownBy(() -> new CustomerName(" "))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("O nome do cliente deve ter entre 1 e 120 caracteres.");
        assertThatThrownBy(() -> new CustomerName(oversizedCustomerName))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("O nome do cliente deve ter entre 1 e 120 caracteres.");
        assertThatThrownBy(() -> new ComplaintText(null))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("O texto da reclamação é obrigatório.");
        assertThatThrownBy(() -> new ComplaintText(" "))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("O texto da reclamação deve ter entre 1 e 4000 caracteres.");
        assertThatThrownBy(() -> new ComplaintText(oversizedComplaintText))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("O texto da reclamação deve ter entre 1 e 4000 caracteres.");
    }

    @Test
    void shouldRejectInvalidDocumentUrlsStatusesAndCategories() {
        Long missingKeywordId = missingKeywordId();
        String missingKeywordValue = missingKeywordValue();

        assertThatThrownBy(() -> new DocumentUrl(null))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("A URL do documento é obrigatória.");
        assertThatThrownBy(() -> new DocumentUrl("ftp://example.com"))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("A URL do documento deve ser HTTP ou HTTPS absoluta.");
        assertThatThrownBy(() -> new DocumentUrl("relative/path"))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("A URL do documento deve ser HTTP ou HTTPS absoluta.");
        assertThatThrownBy(() -> new DocumentUrl("https:///sem-host"))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("A URL do documento deve ser HTTP ou HTTPS absoluta.");
        assertThatThrownBy(() -> new DocumentUrl("://broken"))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("A URL do documento deve ser HTTP ou HTTPS absoluta.");
        assertThatThrownBy(() -> ComplaintStatus.fromId(99))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("O status da reclamação é inválido.");
        assertThatThrownBy(() -> new CategoryKeyword(missingKeywordId, "login"))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("O identificador da palavra-chave da categoria é obrigatório.");
        assertThatThrownBy(() -> new CategoryKeyword(1L, missingKeywordValue))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("A palavra-chave da categoria é obrigatória.");
        assertThatThrownBy(() -> new CategoryKeyword(1L, " "))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("A palavra-chave da categoria é obrigatória.");
    }

    @Test
    void shouldRejectInvalidCategoryAndCustomerConstruction() {
        Category.Builder missingCategoryIdBuilder = Category.builder().name("acesso");
        Category.Builder missingCategoryNameBuilder = Category.builder().id(1L);
        Category.Builder blankCategoryNameBuilder = Category.builder().id(1L).name(" ").keywords(Set.of());
        Customer.Builder missingCpfBuilder = Customer.builder()
                .name(new CustomerName("Maria"))
                .birthDate(LocalDate.of(1990, 6, 15))
                .emailAddress(new EmailAddress("maria@example.com"));
        Customer.Builder missingNameBuilder = Customer.builder()
                .cpf(new Cpf("52998224725"))
                .birthDate(LocalDate.of(1990, 6, 15))
                .emailAddress(new EmailAddress("maria@example.com"));
        Customer.Builder missingBirthDateBuilder = Customer.builder()
                .cpf(new Cpf("52998224725"))
                .name(new CustomerName("Maria"))
                .emailAddress(new EmailAddress("maria@example.com"));
        Customer.Builder missingEmailBuilder = Customer.builder()
                .cpf(new Cpf("52998224725"))
                .name(new CustomerName("Maria"))
                .birthDate(LocalDate.of(1990, 6, 15));
        Customer.Builder futureBirthDateBuilder = Customer.builder()
                .cpf(new Cpf("52998224725"))
                .name(new CustomerName("Maria"))
                .birthDate(LocalDate.now().plusDays(1))
                .emailAddress(new EmailAddress("maria@example.com"));

        assertThatThrownBy(missingCategoryIdBuilder::build)
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("O identificador da categoria é obrigatório.");
        assertThatThrownBy(missingCategoryNameBuilder::build)
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("O nome da categoria é obrigatório.");
        assertThatThrownBy(blankCategoryNameBuilder::build)
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("O nome da categoria é obrigatório.");
        assertThatThrownBy(missingCpfBuilder::build)
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("O CPF do cliente é obrigatório.");
        assertThatThrownBy(missingNameBuilder::build)
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("O nome do cliente é obrigatório.");
        assertThatThrownBy(missingBirthDateBuilder::build)
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("A data de nascimento do cliente é obrigatória.");
        assertThatThrownBy(missingEmailBuilder::build)
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("O e-mail do cliente é obrigatório.");
        assertThatThrownBy(futureBirthDateBuilder::build)
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("A data de nascimento do cliente não pode ser futura.");
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

        Complaint restoredComplaint = Complaint.builder()
                .id(new ComplaintId(UUID.fromString("11111111-1111-1111-1111-111111111111")))
                .customer(TestFixtures.customer())
                .complaintDate(LocalDate.of(2026, 3, 20))
                .complaintText(new ComplaintText("texto"))
                .documentUrls(null)
                .status(ComplaintStatus.RESOLVED)
                .categories(null)
                .registeredAt(TestFixtures.FIXED_CLOCK.instant())
                .buildReconstituted();

        assertThat(restoredComplaint.id().value()).isEqualTo(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        assertThat(restoredComplaint.status()).isEqualTo(ComplaintStatus.RESOLVED);
        assertThat(restoredComplaint.documentUrls()).isEmpty();
        assertThat(restoredComplaint.categories()).isEmpty();
        assertThat(restoredComplaint.pullDomainEvents()).isEmpty();
    }

    @Test
    void shouldRejectIncompleteComplaintConstruction() {
        Complaint.Builder missingClockBuilder = Complaint.builder()
                .customer(TestFixtures.customer())
                .complaintDate(LocalDate.of(2026, 3, 20))
                .complaintText(new ComplaintText("texto"))
                .categories(Set.of())
                ;
        Complaint.Builder missingIdBuilder = Complaint.builder()
                .customer(TestFixtures.customer())
                .complaintDate(LocalDate.of(2026, 3, 20))
                .complaintText(new ComplaintText("texto"))
                .status(ComplaintStatus.PENDING)
                .registeredAt(Instant.now());
        Complaint.Builder missingCustomerBuilder = Complaint.builder()
                .id(ComplaintId.newId())
                .complaintDate(LocalDate.of(2026, 3, 20))
                .complaintText(new ComplaintText("texto"))
                .status(ComplaintStatus.PENDING)
                .registeredAt(Instant.now());
        Complaint.Builder missingComplaintDateBuilder = Complaint.builder()
                .id(ComplaintId.newId())
                .customer(TestFixtures.customer())
                .complaintText(new ComplaintText("texto"))
                .status(ComplaintStatus.PENDING)
                .registeredAt(Instant.now());
        Complaint.Builder missingComplaintTextBuilder = Complaint.builder()
                .id(ComplaintId.newId())
                .customer(TestFixtures.customer())
                .complaintDate(LocalDate.of(2026, 3, 20))
                .status(ComplaintStatus.PENDING)
                .registeredAt(Instant.now());
        Complaint.Builder missingStatusBuilder = Complaint.builder()
                .id(ComplaintId.newId())
                .customer(TestFixtures.customer())
                .complaintDate(LocalDate.of(2026, 3, 20))
                .complaintText(new ComplaintText("texto"))
                .registeredAt(Instant.now());
        Complaint.Builder missingRegisteredAtBuilder = Complaint.builder()
                .id(ComplaintId.newId())
                .customer(TestFixtures.customer())
                .complaintDate(LocalDate.of(2026, 3, 20))
                .complaintText(new ComplaintText("texto"))
                .status(ComplaintStatus.PENDING);

        assertThatThrownBy(missingClockBuilder::buildNew)
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("O relógio de criação da reclamação é obrigatório.");
        assertThatThrownBy(missingIdBuilder::buildReconstituted)
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("O identificador da reclamação é obrigatório.");
        assertThatThrownBy(missingCustomerBuilder::buildReconstituted)
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("O cliente da reclamação é obrigatório.");
        assertThatThrownBy(missingComplaintDateBuilder::buildReconstituted)
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("A data da reclamação é obrigatória.");
        assertThatThrownBy(missingComplaintTextBuilder::buildReconstituted)
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("O texto da reclamação é obrigatório.");
        assertThatThrownBy(missingStatusBuilder::buildReconstituted)
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("O status da reclamação é obrigatório.");
        assertThatThrownBy(missingRegisteredAtBuilder::buildReconstituted)
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("A data de registro da reclamação é obrigatória.");
    }

    @Test
    void shouldAllowSimpleComplaintCreatedDomainEvent() {
        ComplaintCreatedDomainEvent event =
                new ComplaintCreatedDomainEvent(TestFixtures.complaint().id(), TestFixtures.FIXED_CLOCK.instant());
        ComplaintCreatedDomainEvent nullableEvent = new ComplaintCreatedDomainEvent(null, null);

        assertThat(event.complaintId()).isEqualTo(TestFixtures.complaint().id());
        assertThat(event.occurredAt()).isEqualTo(TestFixtures.FIXED_CLOCK.instant());
        assertThat(nullableEvent.complaintId()).isNull();
        assertThat(nullableEvent.occurredAt()).isNull();
    }

    private Long missingKeywordId() {
        return null;
    }

    private String missingKeywordValue() {
        return null;
    }
}
