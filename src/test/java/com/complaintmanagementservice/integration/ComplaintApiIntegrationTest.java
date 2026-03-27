package com.complaintmanagementservice.integration;

import com.complaintmanagementservice.adapters.in.rest.dto.ComplaintSearchResponse;
import com.complaintmanagementservice.adapters.in.rest.dto.CreateComplaintRestResponse;
import com.complaintmanagementservice.adapters.out.messaging.dto.ComplaintCreatedQueueMessage;
import com.complaintmanagementservice.adapters.out.persistence.entity.CategoryEntity;
import com.complaintmanagementservice.adapters.out.persistence.entity.ComplaintEntity;
import com.complaintmanagementservice.domain.model.Complaint;
import com.complaintmanagementservice.domain.model.ComplaintStatus;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.task.scheduling.enabled=false")
@Import(IntegrationTestConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ComplaintApiIntegrationTest extends IntegrationTestSupport {

    @Test
    void shouldCreateComplaintThroughRestAndPublishCreatedEvent() throws Exception {
        MvcResult result = mockMvc.perform(post("/complaints")
                        .contentType(APPLICATION_JSON)
                        .content(validRestPayloadWithDocuments()))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.statusId").value(1))
                .andExpect(jsonPath("$.statusName").value("PENDING"))
                .andReturn();

        CreateComplaintRestResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                CreateComplaintRestResponse.class
        );
        UUID complaintId = UUID.fromString(response.complaintId());

        awaitCondition(() -> complaintJpaRepository.count() == 1L, "Complaint was not persisted after REST creation");

        ComplaintEntity complaintEntity = loadComplaintEntity(complaintId);
        ComplaintCreatedQueueMessage createdEvent = awaitCreatedEvent();

        assertThat(customerJpaRepository.findById("52998224725")).isPresent();
        assertThat(complaintEntity.getStatus().getId()).isEqualTo(ComplaintStatus.PENDING.id());
        assertThat(complaintEntity.getStatus().getName()).isEqualTo("PENDING");
        assertThat(complaintEntity.getComplaintDate()).isEqualTo(LocalDate.of(2026, 3, 20));
        assertThat(complaintEntity.getCategories()).extracting(CategoryEntity::getName)
                .containsExactlyInAnyOrder("acesso", "aplicativo", "cobrança", "fraude");
        assertThat(loadPersistedDocumentUrls(complaintId))
                .containsExactly("https://example.com/doc-1", "https://example.com/doc-2");
        assertThat(createdEvent.complaintId()).isEqualTo(complaintId);
        assertThat(createdEvent.createdAt()).isEqualTo(REFERENCE_INSTANT);
    }

    @Test
    void shouldCreateComplaintWithoutDocumentUrls() throws Exception {
        MvcResult result = mockMvc.perform(post("/complaints")
                        .contentType(APPLICATION_JSON)
                        .content(validRestPayloadWithoutDocuments()))
                .andExpect(status().isCreated())
                .andReturn();

        CreateComplaintRestResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                CreateComplaintRestResponse.class
        );

        awaitCondition(() -> complaintJpaRepository.count() == 1L, "Complaint without documents was not persisted");

        assertThat(loadPersistedDocumentUrls(UUID.fromString(response.complaintId()))).isEmpty();
    }

    @Test
    void shouldReturnBadRequestWhenPostPayloadIsInvalid() throws Exception {
        mockMvc.perform(post("/complaints")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "customer": {
                                    "cpf": "",
                                    "name": "",
                                    "birthDate": null,
                                    "email": "email-invalido"
                                  },
                                  "complaintCreatedDate": null,
                                  "complaintText": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Dados inválidos"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.length()").value(7))
                .andExpect(jsonPath("$.errors[?(@.field=='complaintText')].message").value("Não pode ser nulo ou vazio"))
                .andExpect(jsonPath("$.errors[?(@.field=='customer.email')].message").value("Formato inválido"));
    }

    @Test
    void shouldReturnBadRequestWhenSearchDateCannotBeParsed() throws Exception {
        mockMvc.perform(get("/complaints").param("startDate", "2026-99-99"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Dados inválidos"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors[0].field").value("startDate"))
                .andExpect(jsonPath("$.errors[0].message").value("Data inválida"));
    }

    @Test
    void shouldReturnUnprocessableEntityWhenPostViolatesDomainValidation() throws Exception {
        mockMvc.perform(post("/complaints")
                        .contentType(APPLICATION_JSON)
                        .content(complaintRestPayload(
                                "Não consigo acessar o aplicativo.",
                                List.of("https:///sem-host")
                        )))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.title").value("Regra de negócio violada"))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.message").value("A URL do documento deve ser HTTP ou HTTPS absoluta."));

        assertThat(complaintJpaRepository.count()).isZero();
    }

    @Test
    void shouldReturnUnprocessableEntityWhenSearchDateRangeIsInvalid() throws Exception {
        mockMvc.perform(get("/complaints")
                        .param("startDate", "2026-03-25")
                        .param("endDate", "2026-03-20"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.title").value("Regra de negócio violada"))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.message").value("A data inicial deve ser menor ou igual à data final."));
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    void shouldReturnNotFoundWhenReferenceCatalogIsMissing() throws Exception {
        jdbcTemplate.update("DELETE FROM category_keywords");
        jdbcTemplate.update("DELETE FROM categories");

        mockMvc.perform(post("/complaints")
                        .contentType(APPLICATION_JSON)
                        .content(validRestPayloadWithoutDocuments()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso não encontrado"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("O catálogo de categorias de reclamação não está configurado."));
    }

    @Test
    void shouldReturnAllComplaintsWithoutFiltersOrderedByNewestDateFirst() throws Exception {
        Complaint newestComplaint = seedComplaint("52998224725", "Maria da Silva", "maria.silva@example.com",
                LocalDate.of(2026, 3, 22), ComplaintStatus.PENDING, Set.of("acesso"), List.of("https://example.com/a"));
        Complaint middleComplaint = seedComplaint("11144477735", "João Souza", "joao.souza@example.com",
                LocalDate.of(2026, 3, 21), ComplaintStatus.RESOLVED, Set.of("seguros"), List.of());
        Complaint oldestComplaint = seedComplaint("52998224725", "Maria da Silva", "maria.silva@example.com",
                LocalDate.of(2026, 3, 20), ComplaintStatus.PROCESSING, Set.of("fraude", "cobrança"), List.of());

        ComplaintSearchResponse[] responses = searchComplaints();

        assertThat(responses).hasSize(3);
        assertThat(List.of(responses)).extracting(ComplaintSearchResponse::complaintId)
                .containsExactly(
                        newestComplaint.id().value().toString(),
                        middleComplaint.id().value().toString(),
                        oldestComplaint.id().value().toString()
                );
        assertThat(responses[0].customer().cpf()).isEqualTo("52998224725");
        assertThat(responses[0].documentUrls()).containsExactly("https://example.com/a");
    }

    @Test
    void shouldFilterComplaintsByCustomerCpf() throws Exception {
        seedSearchDataset();

        ComplaintSearchResponse[] responses = searchComplaints("customerCpf", "52998224725");

        assertThat(responses).hasSize(2);
        assertThat(List.of(responses)).extracting(response -> response.customer().cpf())
                .containsOnly("52998224725");
    }

    @Test
    void shouldFilterComplaintsByCategories() throws Exception {
        Complaint targetComplaint = seedSearchDataset().oldestComplaint();

        ComplaintSearchResponse[] responses = searchComplaints("categories", "fraude");

        assertThat(responses).hasSize(1);
        assertThat(responses[0].complaintId()).isEqualTo(targetComplaint.id().value().toString());
        assertThat(responses[0].categories()).extracting(ComplaintSearchResponse.CategoryPayload::name)
                .contains("fraude");
    }

    @Test
    void shouldFilterComplaintsByStatus() throws Exception {
        Complaint targetComplaint = seedSearchDataset().middleComplaint();

        ComplaintSearchResponse[] responses = searchComplaints("status", String.valueOf(ComplaintStatus.RESOLVED.id()));

        assertThat(responses).hasSize(1);
        assertThat(responses[0].complaintId()).isEqualTo(targetComplaint.id().value().toString());
        assertThat(responses[0].status().name()).isEqualTo("RESOLVED");
    }

    @Test
    void shouldFilterComplaintsByStartDate() throws Exception {
        seedSearchDataset();

        ComplaintSearchResponse[] responses = searchComplaints("startDate", "2026-03-21");

        assertThat(responses).hasSize(2);
        assertThat(List.of(responses)).extracting(ComplaintSearchResponse::complaintCreatedDate)
                .containsExactly(LocalDate.of(2026, 3, 22), LocalDate.of(2026, 3, 21));
    }

    @Test
    void shouldFilterComplaintsByEndDate() throws Exception {
        seedSearchDataset();

        ComplaintSearchResponse[] responses = searchComplaints("endDate", "2026-03-21");

        assertThat(responses).hasSize(2);
        assertThat(List.of(responses)).extracting(ComplaintSearchResponse::complaintCreatedDate)
                .containsExactly(LocalDate.of(2026, 3, 21), LocalDate.of(2026, 3, 20));
    }

    @Test
    void shouldFilterComplaintsByDateRange() throws Exception {
        seedSearchDataset();

        ComplaintSearchResponse[] responses = searchComplaints(
                new String[][]{
                        {"startDate", "2026-03-21"},
                        {"endDate", "2026-03-22"}
                }
        );

        assertThat(responses).hasSize(2);
        assertThat(List.of(responses)).extracting(ComplaintSearchResponse::complaintCreatedDate)
                .containsExactly(LocalDate.of(2026, 3, 22), LocalDate.of(2026, 3, 21));
    }

    @Test
    void shouldFilterComplaintsUsingCombinedFilters() throws Exception {
        Complaint targetComplaint = seedSearchDataset().oldestComplaint();

        ComplaintSearchResponse[] responses = searchComplaints(
                new String[][]{
                        {"customerCpf", "52998224725"},
                        {"categories", "fraude"},
                        {"status", String.valueOf(ComplaintStatus.PROCESSING.id())},
                        {"startDate", "2026-03-19"},
                        {"endDate", "2026-03-21"}
                }
        );

        assertThat(responses).hasSize(1);
        assertThat(responses[0].complaintId()).isEqualTo(targetComplaint.id().value().toString());
        assertThat(responses[0].status().id()).isEqualTo(ComplaintStatus.PROCESSING.id());
    }

    private SeededComplaints seedSearchDataset() {
        Complaint newestComplaint = seedComplaint(
                "52998224725",
                "Maria da Silva",
                "maria.silva@example.com",
                LocalDate.of(2026, 3, 22),
                ComplaintStatus.PENDING,
                Set.of("acesso"),
                List.of("https://example.com/a")
        );
        Complaint middleComplaint = seedComplaint(
                "11144477735",
                "João Souza",
                "joao.souza@example.com",
                LocalDate.of(2026, 3, 21),
                ComplaintStatus.RESOLVED,
                Set.of("seguros"),
                List.of()
        );
        Complaint oldestComplaint = seedComplaint(
                "52998224725",
                "Maria da Silva",
                "maria.silva@example.com",
                LocalDate.of(2026, 3, 20),
                ComplaintStatus.PROCESSING,
                Set.of("fraude", "cobrança"),
                List.of()
        );
        return new SeededComplaints(newestComplaint, middleComplaint, oldestComplaint);
    }

    private Complaint seedComplaint(
            String cpf,
            String customerName,
            String customerEmail,
            LocalDate complaintDate,
            ComplaintStatus status,
            Set<String> categoryNames,
            List<String> documentUrls
    ) {
        return persistComplaint(
                cpf,
                customerName,
                customerEmail,
                LocalDate.of(1990, 6, 15),
                complaintDate,
                "Texto de teste da reclamação",
                status,
                categoryNames,
                documentUrls
        );
    }

    private ComplaintSearchResponse[] searchComplaints(String parameterName, String parameterValue) throws Exception {
        return searchComplaints(new String[][]{{parameterName, parameterValue}});
    }

    private ComplaintSearchResponse[] searchComplaints() throws Exception {
        return searchComplaints(new String[0][0]);
    }

    private ComplaintSearchResponse[] searchComplaints(String[][] parameters) throws Exception {
        var requestBuilder = get("/complaints");
        for (String[] parameter : parameters) {
            requestBuilder.param(parameter[0], parameter[1]);
        }

        MvcResult result = mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readValue(result.getResponse().getContentAsString(), ComplaintSearchResponse[].class);
    }

    private record SeededComplaints(Complaint newestComplaint, Complaint middleComplaint, Complaint oldestComplaint) {
    }
}
