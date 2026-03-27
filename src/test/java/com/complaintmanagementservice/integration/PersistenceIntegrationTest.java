package com.complaintmanagementservice.integration;

import com.complaintmanagementservice.application.query.SearchComplaintsQuery;
import com.complaintmanagementservice.domain.model.Complaint;
import com.complaintmanagementservice.domain.model.ComplaintStatus;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.task.scheduling.enabled=false")
@Import(IntegrationTestConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PersistenceIntegrationTest extends IntegrationTestSupport {

    @Test
    void shouldLoadReferenceDataWithFlyway() {
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM complaint_statuses", Integer.class)).isEqualTo(3);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM categories", Integer.class)).isEqualTo(6);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM category_keywords", Integer.class)).isEqualTo(20);
        assertThat(jdbcTemplate.queryForList("SELECT name FROM complaint_statuses ORDER BY id", String.class))
                .containsExactly("PENDING", "PROCESSING", "RESOLVED");
    }

    @Test
    void shouldPersistComplaintGraphWithCustomerStatusCategoriesAndDocuments() {
        Complaint complaint = persistComplaint(
                "52998224725",
                "Maria da Silva",
                "maria.silva@example.com",
                LocalDate.of(1990, 6, 15),
                LocalDate.of(2026, 3, 20),
                "Texto persistido para integração",
                ComplaintStatus.PROCESSING,
                Set.of("acesso", "fraude"),
                List.of("https://example.com/doc-1", "https://example.com/doc-2")
        );

        var complaintEntity = loadComplaintEntity(complaint.id().value());

        assertThat(customerJpaRepository.findById("52998224725")).isPresent();
        assertThat(complaintEntity.getCustomer().getCpf()).isEqualTo("52998224725");
        assertThat(complaintEntity.getStatus().getId()).isEqualTo(ComplaintStatus.PROCESSING.id());
        assertThat(complaintEntity.getCategories()).extracting("name")
                .containsExactlyInAnyOrder("acesso", "fraude");
        assertThat(loadPersistedDocumentUrls(complaint.id().value()))
                .containsExactly("https://example.com/doc-1", "https://example.com/doc-2");
    }

    @Test
    void shouldSearchComplaintsThroughDatabaseUsingSupportedFiltersAndOrdering() {
        Complaint newestComplaint = persistComplaint(
                "52998224725",
                "Maria da Silva",
                "maria.silva@example.com",
                LocalDate.of(1990, 6, 15),
                LocalDate.of(2026, 3, 22),
                "Reclamação mais nova",
                ComplaintStatus.PENDING,
                Set.of("acesso"),
                List.of()
        );
        Complaint middleComplaint = persistComplaint(
                "11144477735",
                "João Souza",
                "joao.souza@example.com",
                LocalDate.of(1988, 2, 10),
                LocalDate.of(2026, 3, 21),
                "Reclamação do seguro",
                ComplaintStatus.RESOLVED,
                Set.of("seguros"),
                List.of()
        );
        Complaint oldestComplaint = persistComplaint(
                "52998224725",
                "Maria da Silva",
                "maria.silva@example.com",
                LocalDate.of(1990, 6, 15),
                LocalDate.of(2026, 3, 20),
                "Reclamação com fraude",
                ComplaintStatus.PROCESSING,
                Set.of("fraude", "cobrança"),
                List.of()
        );

        List<Complaint> allComplaints = complaintRepositoryPort.search(SearchComplaintsQuery.builder().build());
        List<Complaint> complaintsByCpf = complaintRepositoryPort.search(SearchComplaintsQuery.builder()
                .customerCpf("52998224725")
                .build());
        List<Complaint> complaintsByCategory = complaintRepositoryPort.search(SearchComplaintsQuery.builder()
                .categoryNames(List.of("fraude"))
                .build());
        List<Complaint> complaintsByStatus = complaintRepositoryPort.search(SearchComplaintsQuery.builder()
                .statusIds(List.of(ComplaintStatus.RESOLVED.id()))
                .build());
        List<Complaint> complaintsByStartDate = complaintRepositoryPort.search(SearchComplaintsQuery.builder()
                .startDate(LocalDate.of(2026, 3, 21))
                .build());
        List<Complaint> complaintsByEndDate = complaintRepositoryPort.search(SearchComplaintsQuery.builder()
                .endDate(LocalDate.of(2026, 3, 21))
                .build());
        List<Complaint> complaintsByRange = complaintRepositoryPort.search(SearchComplaintsQuery.builder()
                .startDate(LocalDate.of(2026, 3, 21))
                .endDate(LocalDate.of(2026, 3, 22))
                .build());
        List<Complaint> complaintsByCombination = complaintRepositoryPort.search(SearchComplaintsQuery.builder()
                .customerCpf("52998224725")
                .categoryNames(List.of("fraude"))
                .statusIds(List.of(ComplaintStatus.PROCESSING.id()))
                .startDate(LocalDate.of(2026, 3, 19))
                .endDate(LocalDate.of(2026, 3, 21))
                .build());

        assertThat(allComplaints).extracting(Complaint::id)
                .containsExactly(newestComplaint.id(), middleComplaint.id(), oldestComplaint.id());
        assertThat(complaintsByCpf).extracting(Complaint::id)
                .containsExactly(newestComplaint.id(), oldestComplaint.id());
        assertThat(complaintsByCategory).extracting(Complaint::id)
                .containsExactly(oldestComplaint.id());
        assertThat(complaintsByStatus).extracting(Complaint::id)
                .containsExactly(middleComplaint.id());
        assertThat(complaintsByStartDate).extracting(Complaint::id)
                .containsExactly(newestComplaint.id(), middleComplaint.id());
        assertThat(complaintsByEndDate).extracting(Complaint::id)
                .containsExactly(middleComplaint.id(), oldestComplaint.id());
        assertThat(complaintsByRange).extracting(Complaint::id)
                .containsExactly(newestComplaint.id(), middleComplaint.id());
        assertThat(complaintsByCombination).extracting(Complaint::id)
                .containsExactly(oldestComplaint.id());
    }
}
