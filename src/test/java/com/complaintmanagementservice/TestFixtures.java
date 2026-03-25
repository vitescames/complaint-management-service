package com.complaintmanagementservice;

import com.complaintmanagementservice.application.command.CreateComplaintCommand;
import com.complaintmanagementservice.application.notification.ComplaintSlaWarningNotification;
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
import java.util.Set;
import java.util.UUID;

public final class TestFixtures {

    public static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-03-23T10:15:30Z"), ZoneOffset.UTC);

    private TestFixtures() {
    }

    public static Customer customer() {
        return Customer.builder()
                .cpf(new Cpf("52998224725"))
                .name(new CustomerName("Maria Silva"))
                .birthDate(LocalDate.of(1990, 6, 15))
                .emailAddress(new EmailAddress("maria.silva@example.com"))
                .build();
    }

    public static Category acessoCategory() {
        return Category.builder()
                .id(4L)
                .name("acesso")
                .keywords(Set.of(
                        new CategoryKeyword(11L, "acessar"),
                        new CategoryKeyword(12L, "login"),
                        new CategoryKeyword(13L, "senha")
                ))
                .build();
    }

    public static Category cobrancaCategory() {
        return Category.builder()
                .id(3L)
                .name("cobranca")
                .keywords(Set.of(
                        new CategoryKeyword(7L, "fatura"),
                        new CategoryKeyword(8L, "cobranca")
                ))
                .build();
    }

    public static Complaint complaint() {
        return Complaint.reconstitute(
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
        return Complaint.builder()
                .customer(customer())
                .complaintDate(LocalDate.of(2026, 3, 20))
                .complaintText(new ComplaintText("Nao consigo acessar o app e a fatura esta indevida"))
                .documentUrls(List.of(new DocumentUrl("https://example.com/doc-1")))
                .categories(Set.of(acessoCategory(), cobrancaCategory()))
                .clock(FIXED_CLOCK)
                .buildNew();
    }

    public static CreateComplaintCommand createComplaintCommand() {
        return CreateComplaintCommand.builder()
                .customerCpf("52998224725")
                .customerName("Maria Silva")
                .customerBirthDate(LocalDate.of(1990, 6, 15))
                .customerEmail("maria.silva@example.com")
                .complaintCreatedDate(LocalDate.of(2026, 3, 20))
                .complaintText("Meu login falha e a fatura veio errada")
                .documentUrls(List.of("https://example.com/doc-1"))
                .build();
    }

    public static SearchComplaintsQuery searchQuery() {
        return SearchComplaintsQuery.builder()
                .customerCpf("52998224725")
                .categoryNames(List.of("acesso", "cobranca"))
                .statusIds(List.of(ComplaintStatus.PENDING.id()))
                .startDate(LocalDate.of(2026, 3, 1))
                .endDate(LocalDate.of(2026, 3, 31))
                .build();
    }

    public static ComplaintSlaWarningNotification complaintSlaWarningNotification() {
        return new ComplaintSlaWarningNotification(complaint().id(), LocalDate.of(2026, 3, 30));
    }

    public static ComplaintCreatedDomainEvent complaintCreatedDomainEvent() {
        return new ComplaintCreatedDomainEvent(complaint().id(), Instant.parse("2026-03-23T10:15:30Z"));
    }

    public static Complaint approachingSlaComplaint() {
        return Complaint.reconstitute(
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
