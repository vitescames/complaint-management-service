package com.complaintmanagementservice;

import com.complaintmanagementservice.application.command.CreateComplaintCommand;
import com.complaintmanagementservice.application.model.ComplaintCreatedNotification;
import com.complaintmanagementservice.application.model.ComplaintSlaWarningNotification;
import com.complaintmanagementservice.application.query.SearchComplaintsQuery;
import com.complaintmanagementservice.domain.event.ComplaintCreatedDomainEvent;
import com.complaintmanagementservice.domain.model.Category;
import com.complaintmanagementservice.domain.model.CategoryKeyword;
import com.complaintmanagementservice.domain.model.Complaint;
import com.complaintmanagementservice.domain.model.ComplaintId;
import com.complaintmanagementservice.domain.model.ComplaintStatus;
import com.complaintmanagementservice.domain.model.ComplaintText;
import com.complaintmanagementservice.domain.model.Cpf;
import com.complaintmanagementservice.domain.model.Customer;
import com.complaintmanagementservice.domain.model.CustomerName;
import com.complaintmanagementservice.domain.model.DocumentUrl;
import com.complaintmanagementservice.domain.model.EmailAddress;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class TestFixtures {

    public static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-03-23T10:15:30Z"), ZoneOffset.UTC);

    private TestFixtures() {
    }

    public static Customer customer() {
        return new Customer(
                new Cpf("52998224725"),
                new CustomerName("Maria Silva"),
                LocalDate.of(1990, 6, 15),
                new EmailAddress("maria.silva@example.com")
        );
    }

    public static Category acessoCategory() {
        return new Category(4L, "acesso", Set.of(
                new CategoryKeyword(11L, "acessar"),
                new CategoryKeyword(12L, "login"),
                new CategoryKeyword(13L, "senha")
        ));
    }

    public static Category cobrancaCategory() {
        return new Category(3L, "cobrança", Set.of(
                new CategoryKeyword(7L, "fatura"),
                new CategoryKeyword(8L, "cobrança")
        ));
    }

    public static Complaint complaint() {
        return Complaint.restore(
                new ComplaintId(UUID.fromString("11111111-1111-1111-1111-111111111111")),
                customer(),
                LocalDate.of(2026, 3, 20),
                new ComplaintText("Nao consigo acessar o app e a fatura esta indevida"),
                List.of(new DocumentUrl("https://example.com/doc-1")),
                ComplaintStatus.PENDING,
                Set.of(acessoCategory(), cobrancaCategory()),
                Instant.parse("2026-03-23T10:15:30Z")
        );
    }

    public static Complaint createdComplaint() {
        return Complaint.create(
                customer(),
                LocalDate.of(2026, 3, 20),
                new ComplaintText("Nao consigo acessar o app e a fatura esta indevida"),
                List.of(new DocumentUrl("https://example.com/doc-1")),
                Set.of(acessoCategory(), cobrancaCategory()),
                FIXED_CLOCK
        );
    }

    public static CreateComplaintCommand createComplaintCommand() {
        return new CreateComplaintCommand(
                customer().cpf(),
                customer().name(),
                customer().birthDate(),
                customer().emailAddress(),
                LocalDate.of(2026, 3, 20),
                new ComplaintText("Meu login falha e a fatura veio errada"),
                List.of(new DocumentUrl("https://example.com/doc-1"))
        );
    }

    public static SearchComplaintsQuery searchQuery() {
        return new SearchComplaintsQuery(
                Optional.of(customer().cpf()),
                List.of("acesso", "cobrança"),
                List.of(ComplaintStatus.PENDING),
                Optional.of(LocalDate.of(2026, 3, 1)),
                Optional.of(LocalDate.of(2026, 3, 31))
        );
    }

    public static ComplaintCreatedNotification complaintCreatedNotification() {
        return new ComplaintCreatedNotification(complaint().id(), Instant.parse("2026-03-23T10:15:30Z"));
    }

    public static ComplaintSlaWarningNotification complaintSlaWarningNotification() {
        return new ComplaintSlaWarningNotification(complaint().id(), LocalDate.of(2026, 3, 30));
    }

    public static ComplaintCreatedDomainEvent complaintCreatedDomainEvent() {
        return new ComplaintCreatedDomainEvent(complaint().id(), Instant.parse("2026-03-23T10:15:30Z"));
    }

    public static Complaint approachingSlaComplaint() {
        return Complaint.restore(
                complaint().id(),
                complaint().customer(),
                LocalDate.of(2026, 3, 16),
                complaint().complaintText(),
                complaint().documentUrls(),
                ComplaintStatus.PROCESSING,
                complaint().categories(),
                complaint().registeredAt()
        );
    }
}
