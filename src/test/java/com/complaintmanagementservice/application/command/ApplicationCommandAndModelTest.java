package com.complaintmanagementservice.application.command;

import com.complaintmanagementservice.TestFixtures;
import com.complaintmanagementservice.application.notification.ComplaintSlaWarningNotification;
import com.complaintmanagementservice.application.query.SearchComplaintsQuery;
import com.complaintmanagementservice.domain.event.ComplaintCreatedDomainEvent;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApplicationCommandAndModelTest {

    @Test
    void shouldKeepCommandAndQueryAsPassiveDataCarriers() {
        CreateComplaintCommand command = CreateComplaintCommand.builder()
                .customerCpf("52998224725")
                .customerName("Maria Silva")
                .customerBirthDate(LocalDate.of(1990, 6, 15))
                .customerEmail("maria.silva@example.com")
                .complaintCreatedDate(LocalDate.of(2026, 3, 20))
                .complaintText("Meu login falha")
                .documentUrls(Arrays.asList(" https://example.com/doc-1 ", null))
                .build();
        SearchComplaintsQuery query = SearchComplaintsQuery.builder()
                .customerCpf(" ")
                .categoryNames(Arrays.asList(" acesso ", null, " "))
                .statusIds(Arrays.asList(null, 1))
                .startDate(LocalDate.of(2026, 3, 1))
                .endDate(LocalDate.of(2026, 3, 31))
                .build();

        assertThat(command.customerCpf()).isEqualTo("52998224725");
        assertThat(command.documentUrls()).containsExactly(" https://example.com/doc-1 ");
        assertThat(query.customerCpf()).isEqualTo(" ");
        assertThat(query.categoryNames()).containsExactly(" acesso ", " ");
        assertThat(query.statusIds()).containsExactly(1);
        assertThat(query.startDate()).isEqualTo(LocalDate.of(2026, 3, 1));
        assertThat(query.endDate()).isEqualTo(LocalDate.of(2026, 3, 31));
    }

    @Test
    void shouldDefaultOptionalListsToEmpty() {
        CreateComplaintCommand commandWithoutDocuments = CreateComplaintCommand.builder()
                .customerCpf("52998224725")
                .customerName("Maria Silva")
                .customerBirthDate(LocalDate.of(1990, 6, 15))
                .customerEmail("maria.silva@example.com")
                .complaintCreatedDate(LocalDate.of(2026, 3, 20))
                .complaintText("Meu login falha")
                .documentUrls(null)
                .build();
        SearchComplaintsQuery queryWithoutLists = SearchComplaintsQuery.builder()
                .categoryNames(null)
                .statusIds(null)
                .build();

        assertThat(commandWithoutDocuments.documentUrls()).isEmpty();
        assertThat(queryWithoutLists.categoryNames()).isEmpty();
        assertThat(queryWithoutLists.statusIds()).isEmpty();
    }

    @Test
    void shouldCreateSimpleEventAndNotificationModels() {
        ComplaintCreatedDomainEvent event =
                new ComplaintCreatedDomainEvent(TestFixtures.complaint().id(), TestFixtures.FIXED_CLOCK.instant());
        ComplaintSlaWarningNotification warningNotification =
                new ComplaintSlaWarningNotification(TestFixtures.complaint().id(), LocalDate.of(2026, 3, 30));
        ComplaintCreatedDomainEvent nullableEvent = new ComplaintCreatedDomainEvent(null, null);
        ComplaintSlaWarningNotification nullableNotification = new ComplaintSlaWarningNotification(null, null);

        assertThat(event.complaintId()).isEqualTo(TestFixtures.complaint().id());
        assertThat(warningNotification.slaDeadlineDate()).isEqualTo(LocalDate.of(2026, 3, 30));
        assertThat(nullableEvent.complaintId()).isNull();
        assertThat(nullableEvent.occurredAt()).isNull();
        assertThat(nullableNotification.complaintId()).isNull();
        assertThat(nullableNotification.slaDeadlineDate()).isNull();
    }

    @Test
    void shouldExposeImmutableCommandAndQueryCollections() {
        CreateComplaintCommand command = TestFixtures.createComplaintCommand();
        SearchComplaintsQuery query = TestFixtures.searchQuery();
        List<String> documentUrls = new ArrayList<>(command.documentUrls());
        List<String> categoryNames = new ArrayList<>(query.categoryNames());

        assertThat(documentUrls).isEqualTo(List.of("https://example.com/doc-1"));
        assertThat(categoryNames).containsExactly("acesso", "cobranca");
        assertThat(query.statusIds()).containsExactly(1);
        assertThatThrownBy(() -> documentUrls.add("https://example.com/doc-2"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> categoryNames.add("fraude"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void shouldRejectIncompleteCommandPayload() {
        CreateComplaintCommand.Builder blankCpfBuilder = CreateComplaintCommand.builder()
                .customerCpf(" ")
                .customerName("Maria Silva")
                .customerBirthDate(LocalDate.of(1990, 6, 15))
                .customerEmail("maria.silva@example.com")
                .complaintCreatedDate(LocalDate.of(2026, 3, 20))
                .complaintText("Meu login falha")
                ;
        CreateComplaintCommand.Builder blankTextBuilder = CreateComplaintCommand.builder()
                .customerCpf("52998224725")
                .customerName("Maria Silva")
                .customerBirthDate(LocalDate.of(1990, 6, 15))
                .customerEmail("maria.silva@example.com")
                .complaintCreatedDate(LocalDate.of(2026, 3, 20))
                .complaintText(" ")
                ;
        CreateComplaintCommand.Builder nullBirthDateBuilder = CreateComplaintCommand.builder()
                .customerCpf("52998224725")
                .customerName("Maria Silva")
                .customerBirthDate(null)
                .customerEmail("maria.silva@example.com")
                .complaintCreatedDate(LocalDate.of(2026, 3, 20))
                .complaintText("Meu login falha")
                ;
        CreateComplaintCommand.Builder nullEmailBuilder = CreateComplaintCommand.builder()
                .customerCpf("52998224725")
                .customerName("Maria Silva")
                .customerBirthDate(LocalDate.of(1990, 6, 15))
                .customerEmail(null)
                .complaintCreatedDate(LocalDate.of(2026, 3, 20))
                .complaintText("Meu login falha")
                ;

        assertThatThrownBy(blankCpfBuilder::build)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("O CPF do cliente é obrigatório.");

        assertThatThrownBy(blankTextBuilder::build)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("O texto da reclamação é obrigatório.");

        assertThatThrownBy(nullBirthDateBuilder::build)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A data de nascimento do cliente é obrigatória.");

        assertThatThrownBy(nullEmailBuilder::build)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("O e-mail do cliente é obrigatório.");
    }
}
