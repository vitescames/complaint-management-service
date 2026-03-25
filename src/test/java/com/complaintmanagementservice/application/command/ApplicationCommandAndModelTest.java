package com.complaintmanagementservice.application.command;

import com.complaintmanagementservice.TestFixtures;
import com.complaintmanagementservice.application.exception.ApplicationValidationException;
import com.complaintmanagementservice.application.model.ComplaintCreatedNotification;
import com.complaintmanagementservice.application.model.ComplaintSlaWarningNotification;
import com.complaintmanagementservice.application.query.SearchComplaintsQuery;
import com.complaintmanagementservice.domain.model.ComplaintStatus;
import com.complaintmanagementservice.domain.model.Cpf;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApplicationCommandAndModelTest {

    @Test
    void shouldCreateValidCommandAndQuery() {
        CreateComplaintCommand command = TestFixtures.createComplaintCommand();
        SearchComplaintsQuery query = new SearchComplaintsQuery(
                Optional.of(new Cpf("52998224725")),
                List.of(" acesso ", " ", "cobrança"),
                List.of(ComplaintStatus.PENDING),
                Optional.of(LocalDate.of(2026, 3, 1)),
                Optional.of(LocalDate.of(2026, 3, 31))
        );
        ComplaintCreatedNotification createdNotification =
                new ComplaintCreatedNotification(TestFixtures.complaint().id(), Instant.parse("2026-03-23T10:15:30Z"));
        ComplaintSlaWarningNotification warningNotification =
                new ComplaintSlaWarningNotification(TestFixtures.complaint().id(), LocalDate.of(2026, 3, 30));

        assertThat(command.documentUrls()).hasSize(1);
        assertThat(query.customerCpf()).contains(new Cpf("52998224725"));
        assertThat(query.categoryNames()).containsExactly("acesso", "cobrança");
        assertThat(query.statuses()).containsExactly(ComplaintStatus.PENDING);
        assertThat(query.startDate()).contains(LocalDate.of(2026, 3, 1));
        assertThat(query.endDate()).contains(LocalDate.of(2026, 3, 31));
        assertThat(createdNotification.createdAt()).isEqualTo(Instant.parse("2026-03-23T10:15:30Z"));
        assertThat(warningNotification.slaDeadlineDate()).isEqualTo(LocalDate.of(2026, 3, 30));
    }

    @Test
    void shouldRejectInvalidCommandAndQuery() {
        assertThatThrownBy(() -> new CreateComplaintCommand(
                TestFixtures.customer().cpf(),
                TestFixtures.customer().name(),
                TestFixtures.customer().birthDate(),
                TestFixtures.customer().emailAddress(),
                LocalDate.now().plusDays(1),
                TestFixtures.complaint().complaintText(),
                null
        )).isInstanceOf(ApplicationValidationException.class)
                .hasMessage("Complaint created date cannot be in the future");

        assertThatThrownBy(() -> new SearchComplaintsQuery(
                null,
                null,
                null,
                Optional.of(LocalDate.of(2026, 4, 1)),
                Optional.of(LocalDate.of(2026, 3, 1))
        )).isInstanceOf(ApplicationValidationException.class)
                .hasMessage("Start date must be before or equal to end date");

        assertThatThrownBy(() -> new ComplaintCreatedNotification(null, Instant.now()))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("complaintId must not be null");
        assertThatThrownBy(() -> new ComplaintSlaWarningNotification(TestFixtures.complaint().id(), null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("slaDeadlineDate must not be null");

        SearchComplaintsQuery partialDateQuery = new SearchComplaintsQuery(
                null,
                null,
                null,
                Optional.of(LocalDate.of(2026, 3, 1)),
                null
        );

        assertThat(partialDateQuery.customerCpf()).isEmpty();
        assertThat(partialDateQuery.categoryNames()).isEmpty();
        assertThat(partialDateQuery.statuses()).isEmpty();
        assertThat(partialDateQuery.startDate()).contains(LocalDate.of(2026, 3, 1));
        assertThat(partialDateQuery.endDate()).isEmpty();
    }
}
