package com.complaintmanagementservice.application.command;

import com.complaintmanagementservice.TestFixtures;
import com.complaintmanagementservice.application.notification.ComplaintSlaWarningNotification;
import com.complaintmanagementservice.application.query.SearchComplaintsQuery;
import com.complaintmanagementservice.domain.event.ComplaintCreatedDomainEvent;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
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
        assertThat(command.documentUrls()).containsExactly(" https://example.com/doc-1 ", null);
        assertThat(query.customerCpf()).isEqualTo(" ");
        assertThat(query.categoryNames()).containsExactly(" acesso ", null, " ");
        assertThat(query.statusIds()).containsExactly(null, 1);
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

        assertThat(command.documentUrls()).isEqualTo(List.of("https://example.com/doc-1"));
        assertThat(query.categoryNames()).containsExactly("acesso", "cobranca");
        assertThat(query.statusIds()).containsExactly(1);
        assertThatThrownBy(() -> command.documentUrls().add("https://example.com/doc-2"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> query.categoryNames().add("fraude"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
