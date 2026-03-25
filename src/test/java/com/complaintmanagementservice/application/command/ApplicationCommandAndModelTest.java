package com.complaintmanagementservice.application.command;

import com.complaintmanagementservice.TestFixtures;
import com.complaintmanagementservice.application.exception.BusinessRuleViolationException;
import com.complaintmanagementservice.application.exception.RequestValidationException;
import com.complaintmanagementservice.application.notification.ComplaintSlaWarningNotification;
import com.complaintmanagementservice.application.query.SearchComplaintsQuery;
import com.complaintmanagementservice.domain.exception.DomainValidationException;
import com.complaintmanagementservice.domain.model.ComplaintStatus;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApplicationCommandAndModelTest {

    @Test
    void shouldCreateValidCommandAndQuery() {
        CreateComplaintCommand command = TestFixtures.createComplaintCommand();
        CreateComplaintCommand commandWithoutDocuments = CreateComplaintCommand.builder()
                .customerCpf("52998224725")
                .customerName("Maria Silva")
                .customerBirthDate(LocalDate.of(1990, 6, 15))
                .customerEmail("maria.silva@example.com")
                .complaintCreatedDate(LocalDate.of(2026, 3, 20))
                .complaintText("Meu login falha")
                .documentUrls(null)
                .build();
        SearchComplaintsQuery query = SearchComplaintsQuery.builder()
                .customerCpf("52998224725")
                .categoryNames(Arrays.asList(" acesso ", null, " ", "cobranca"))
                .statusIds(Arrays.asList(null, ComplaintStatus.PENDING.id()))
                .startDate(LocalDate.of(2026, 3, 1))
                .endDate(LocalDate.of(2026, 3, 31))
                .build();
        ComplaintSlaWarningNotification warningNotification =
                new ComplaintSlaWarningNotification(TestFixtures.complaint().id(), LocalDate.of(2026, 3, 30));
        SearchComplaintsQuery queryWithoutLists = SearchComplaintsQuery.builder()
                .categoryNames(null)
                .statusIds(null)
                .build();

        assertThat(command.documentUrls()).hasSize(1);
        assertThat(commandWithoutDocuments.documentUrls()).isEmpty();
        assertThat(query.customerCpf()).isEqualTo("52998224725");
        assertThat(query.categoryNames()).containsExactly("acesso", "cobranca");
        assertThat(query.statusIds()).containsExactly(ComplaintStatus.PENDING.id());
        assertThat(queryWithoutLists.categoryNames()).isEmpty();
        assertThat(queryWithoutLists.statusIds()).isEmpty();
        assertThat(query.startDate()).isEqualTo(LocalDate.of(2026, 3, 1));
        assertThat(query.endDate()).isEqualTo(LocalDate.of(2026, 3, 31));
        assertThat(warningNotification.slaDeadlineDate()).isEqualTo(LocalDate.of(2026, 3, 30));
    }

    @Test
    void shouldRejectInvalidCommandAndQuery() {
        assertThatThrownBy(() -> CreateComplaintCommand.builder()
                .customerName("Maria Silva")
                .customerBirthDate(TestFixtures.customer().birthDate())
                .customerEmail(TestFixtures.customer().emailAddress().value())
                .complaintCreatedDate(LocalDate.of(2026, 3, 20))
                .complaintText("texto")
                .build())
                .isInstanceOf(RequestValidationException.class)
                .hasMessage("O CPF do cliente e obrigatorio");
        assertThatThrownBy(() -> CreateComplaintCommand.builder()
                .customerCpf("52998224725")
                .customerName("Maria Silva")
                .customerEmail(TestFixtures.customer().emailAddress().value())
                .complaintCreatedDate(LocalDate.of(2026, 3, 20))
                .complaintText("texto")
                .build())
                .isInstanceOf(RequestValidationException.class)
                .hasMessage("A data de nascimento do cliente e obrigatoria");
        assertThatThrownBy(() -> CreateComplaintCommand.builder()
                .customerCpf("52998224725")
                .customerName("Maria Silva")
                .customerBirthDate(TestFixtures.customer().birthDate())
                .customerEmail(TestFixtures.customer().emailAddress().value())
                .complaintText("texto")
                .build())
                .isInstanceOf(RequestValidationException.class)
                .hasMessage("A data da reclamacao e obrigatoria");
        assertThatThrownBy(() -> CreateComplaintCommand.builder()
                .customerCpf("52998224725")
                .customerName("Maria Silva")
                .customerBirthDate(TestFixtures.customer().birthDate())
                .customerEmail(TestFixtures.customer().emailAddress().value())
                .complaintCreatedDate(LocalDate.now().plusDays(1))
                .complaintText("texto")
                .build())
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessage("A data da reclamacao nao pode ser futura");
        assertThatThrownBy(() -> CreateComplaintCommand.builder()
                .customerCpf("52998224725")
                .customerName("Maria Silva")
                .customerBirthDate(TestFixtures.customer().birthDate())
                .customerEmail(TestFixtures.customer().emailAddress().value())
                .complaintCreatedDate(LocalDate.of(2026, 3, 20))
                .complaintText("texto")
                .documentUrls(List.of(" "))
                .build())
                .isInstanceOf(RequestValidationException.class)
                .hasMessage("A URL do documento e obrigatoria");

        assertThatThrownBy(() -> SearchComplaintsQuery.builder()
                .customerCpf("invalid")
                .build())
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("CPF invalido");
        assertThatThrownBy(() -> SearchComplaintsQuery.builder()
                .statusIds(List.of(99))
                .build())
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("O status da reclamacao e invalido");
        assertThatThrownBy(() -> SearchComplaintsQuery.builder()
                .startDate(LocalDate.of(2026, 4, 1))
                .endDate(LocalDate.of(2026, 3, 1))
                .build())
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessage("A data inicial deve ser menor ou igual a data final");

        assertThatThrownBy(() -> new ComplaintSlaWarningNotification(null, LocalDate.now()))
                .isInstanceOf(RequestValidationException.class)
                .hasMessage("O identificador da reclamacao e obrigatorio");
        assertThatThrownBy(() -> new ComplaintSlaWarningNotification(TestFixtures.complaint().id(), null))
                .isInstanceOf(RequestValidationException.class)
                .hasMessage("A data limite do SLA e obrigatoria");

        SearchComplaintsQuery partialDateQuery = SearchComplaintsQuery.builder()
                .customerCpf(" ")
                .categoryNames(Arrays.asList(" ", null))
                .statusIds(Collections.singletonList((Integer) null))
                .startDate(LocalDate.of(2026, 3, 1))
                .build();

        assertThat(partialDateQuery.customerCpf()).isNull();
        assertThat(partialDateQuery.categoryNames()).isEmpty();
        assertThat(partialDateQuery.statusIds()).isEmpty();
        assertThat(partialDateQuery.startDate()).isEqualTo(LocalDate.of(2026, 3, 1));
        assertThat(partialDateQuery.endDate()).isNull();
    }
}
