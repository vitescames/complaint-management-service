package com.complaintmanagementservice.integration;

import com.complaintmanagementservice.adapters.in.messaging.dto.CreateComplaintQueueMessage;
import com.complaintmanagementservice.adapters.out.messaging.dto.ComplaintCreatedQueueMessage;
import com.complaintmanagementservice.adapters.out.messaging.dto.ComplaintSlaWarningQueueMessage;
import com.complaintmanagementservice.adapters.out.persistence.ComplaintSpecifications;
import com.complaintmanagementservice.adapters.out.persistence.entity.ComplaintEntity;
import com.complaintmanagementservice.adapters.out.persistence.repository.ComplaintJpaRepository;
import com.complaintmanagementservice.adapters.out.persistence.repository.CustomerJpaRepository;
import com.complaintmanagementservice.application.port.out.CategoryCatalogPort;
import com.complaintmanagementservice.application.port.out.ComplaintRepositoryPort;
import com.complaintmanagementservice.application.query.SearchComplaintsQuery;
import com.complaintmanagementservice.adapters.in.scheduler.SlaWarningScheduler;
import com.complaintmanagementservice.adapters.out.config.MessagingProperties;
import com.complaintmanagementservice.domain.model.Category;
import com.complaintmanagementservice.domain.model.Complaint;
import com.complaintmanagementservice.domain.model.ComplaintId;
import com.complaintmanagementservice.domain.model.ComplaintStatus;
import com.complaintmanagementservice.domain.model.ComplaintText;
import com.complaintmanagementservice.domain.model.Cpf;
import com.complaintmanagementservice.domain.model.Customer;
import com.complaintmanagementservice.domain.model.CustomerName;
import com.complaintmanagementservice.domain.model.DocumentUrl;
import com.complaintmanagementservice.domain.model.EmailAddress;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.jms.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BooleanSupplier;

abstract class IntegrationTestSupport {

    protected static final LocalDate REFERENCE_DATE = LocalDate.of(2026, 3, 23);
    protected static final Instant REFERENCE_INSTANT = Instant.parse("2026-03-23T10:15:30Z");

    @Autowired(required = false)
    protected MockMvc mockMvc;

    @Autowired(required = false)
    private WebApplicationContext webApplicationContext;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected JmsTemplate jmsTemplate;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    protected MessagingProperties messagingProperties;

    @Autowired
    protected ComplaintRepositoryPort complaintRepositoryPort;

    @Autowired
    protected ComplaintJpaRepository complaintJpaRepository;

    @Autowired
    protected CustomerJpaRepository customerJpaRepository;

    @Autowired
    protected CategoryCatalogPort categoryCatalogPort;

    @Autowired
    protected SlaWarningScheduler slaWarningScheduler;

    @DynamicPropertySource
    static void registerIntegrationOverrides(DynamicPropertyRegistry registry) {
        String uniqueSuffix = UUID.randomUUID().toString().replace("-", "");
        registry.add(
                "spring.datasource.url",
                () -> "jdbc:h2:mem:complaintsdb-" + uniqueSuffix + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
        );
        registry.add("application.messaging.broker-name", () -> "complaint-broker-" + uniqueSuffix);
    }

    @org.junit.jupiter.api.BeforeEach
    void prepareIntegrationState() {
        if (mockMvc == null && webApplicationContext != null) {
            mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        }
        jmsTemplate.setReceiveTimeout(100);
        drainKnownQueues();
        clearDynamicTables();
    }

    @org.junit.jupiter.api.AfterEach
    void cleanupIntegrationState() {
        drainKnownQueues();
        clearDynamicTables();
    }

    protected void clearDynamicTables() {
        jdbcTemplate.update("DELETE FROM complaint_documents");
        jdbcTemplate.update("DELETE FROM complaint_categories");
        jdbcTemplate.update("DELETE FROM complaints");
        jdbcTemplate.update("DELETE FROM customers");
    }

    protected void drainKnownQueues() {
        drainQueue(messagingProperties.queues().complaintReceived());
        drainQueue(messagingProperties.queues().complaintCreated());
        drainQueue(messagingProperties.queues().complaintSlaWarning());
        drainQueue(dlqName(messagingProperties.queues().complaintReceived()));
        drainQueue(dlqName(messagingProperties.queues().complaintCreated()));
        drainQueue(dlqName(messagingProperties.queues().complaintSlaWarning()));
    }

    protected void drainQueue(String queueName) {
        while (jmsTemplate.receive(queueName) != null) {
            // Drain queue messages between tests to keep assertions deterministic.
        }
    }

    protected String dlqName(String queueName) {
        return "DLQ." + queueName;
    }

    protected ComplaintEntity loadComplaintEntity(UUID complaintId) {
        return complaintJpaRepository.findAll(
                        ComplaintSpecifications.from(SearchComplaintsQuery.builder().build()),
                        Sort.unsorted()
                ).stream()
                .filter(entity -> entity.getId().equals(complaintId))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Complaint not found in database: " + complaintId));
    }

    protected List<String> loadPersistedDocumentUrls(UUID complaintId) {
        return jdbcTemplate.queryForList(
                "SELECT document_url FROM complaint_documents WHERE complaint_id = ? ORDER BY id",
                String.class,
                complaintId
        );
    }

    protected Complaint persistComplaint(
            String customerCpf,
            String customerName,
            String customerEmail,
            LocalDate customerBirthDate,
            LocalDate complaintDate,
            String complaintText,
            ComplaintStatus status,
            Set<String> categoryNames,
            List<String> documentUrls
    ) {
        Map<Long, Category> categoriesById = categoryCatalogPort.loadAll().stream()
                .collect(java.util.stream.Collectors.toMap(Category::id, category -> category));

        Set<Category> selectedCategories = categoryNames.stream()
                .map(categoryName -> categoryBySemanticName(categoriesById, categoryName))
                .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));

        Complaint complaint = Complaint.builder()
                .id(ComplaintId.newId())
                .customer(Customer.builder()
                        .cpf(new Cpf(customerCpf))
                        .name(new CustomerName(customerName))
                        .birthDate(customerBirthDate)
                        .emailAddress(new EmailAddress(customerEmail))
                        .build())
                .complaintDate(complaintDate)
                .complaintText(new ComplaintText(complaintText))
                .documentUrls(documentUrls.stream().map(DocumentUrl::new).toList())
                .status(status)
                .categories(selectedCategories)
                .registeredAt(REFERENCE_INSTANT.minusSeconds(60))
                .buildReconstituted();

        return complaintRepositoryPort.save(complaint);
    }

    protected CreateComplaintQueueMessage validQueueMessage(String description) {
        return new CreateComplaintQueueMessage(
                "52998224725",
                "Maria da Silva",
                LocalDate.of(1990, 6, 15),
                "maria.silva@example.com",
                LocalDate.of(2026, 3, 20),
                description
        );
    }

    protected String validRestPayloadWithDocuments() {
        return complaintRestPayload(
                "Não consigo acessar o aplicativo. O app está travando e a fatura veio com valor indevido.",
                List.of("https://example.com/doc-1", "https://example.com/doc-2")
        );
    }

    protected String validRestPayloadWithoutDocuments() {
        return complaintRestPayload(
                "Não consigo acessar o aplicativo. O app está travando e a fatura veio com valor indevido.",
                null
        );
    }

    protected String complaintRestPayload(String complaintText, List<String> documentUrls) {
        Map<String, Object> payload = new LinkedHashMap<>();
        Map<String, Object> customer = new LinkedHashMap<>();
        customer.put("cpf", "529.982.247-25");
        customer.put("name", "Maria da Silva");
        customer.put("birthDate", "1990-06-15");
        customer.put("email", "maria.silva@example.com");
        payload.put("customer", customer);
        payload.put("complaintCreatedDate", "2026-03-20");
        payload.put("complaintText", complaintText);
        if (documentUrls != null) {
            payload.put("documentUrls", documentUrls);
        }

        try {
            return objectMapper.writeValueAsString(payload);
        }
        catch (JsonProcessingException exception) {
            throw new AssertionError("Unable to build JSON payload for integration test", exception);
        }
    }

    protected <T> T awaitQueuePayload(String queueName, Class<T> payloadType) {
        long deadline = System.nanoTime() + java.time.Duration.ofSeconds(15).toNanos();
        while (System.nanoTime() < deadline) {
            Object payload = jmsTemplate.receiveAndConvert(queueName);
            if (payload != null) {
                if (!payloadType.isInstance(payload)) {
                    throw new AssertionError("Unexpected queue payload type: " + payload.getClass().getName());
                }
                return payloadType.cast(payload);
            }
            sleepBriefly();
        }
        throw new AssertionError("Timed out waiting for payload on queue: " + queueName);
    }

    protected Message awaitQueueMessage(String queueName) {
        long deadline = System.nanoTime() + java.time.Duration.ofSeconds(15).toNanos();
        while (System.nanoTime() < deadline) {
            Message message = jmsTemplate.receive(queueName);
            if (message != null) {
                return message;
            }
            sleepBriefly();
        }
        throw new AssertionError("Timed out waiting for message on queue: " + queueName);
    }

    protected void awaitCondition(BooleanSupplier condition, String failureMessage) {
        long deadline = System.nanoTime() + java.time.Duration.ofSeconds(15).toNanos();
        while (System.nanoTime() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }
            sleepBriefly();
        }
        throw new AssertionError(failureMessage);
    }

    protected ComplaintCreatedQueueMessage awaitCreatedEvent() {
        return awaitQueuePayload(messagingProperties.queues().complaintCreated(), ComplaintCreatedQueueMessage.class);
    }

    protected ComplaintSlaWarningQueueMessage awaitSlaWarningEvent() {
        return awaitQueuePayload(messagingProperties.queues().complaintSlaWarning(), ComplaintSlaWarningQueueMessage.class);
    }

    private void sleepBriefly() {
        try {
            Thread.sleep(100);
        }
        catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Integration test polling was interrupted", exception);
        }
    }

    private Category categoryBySemanticName(Map<Long, Category> categoriesById, String categoryName) {
        long categoryId = switch (categoryName) {
            case "imobiliario", "imobiliário" -> 1L;
            case "seguros" -> 2L;
            case "cobranca", "cobrança" -> 3L;
            case "acesso" -> 4L;
            case "aplicativo" -> 5L;
            case "fraude" -> 6L;
            default -> throw new AssertionError("Unknown category for integration test: " + categoryName);
        };

        Category category = categoriesById.get(categoryId);
        if (category == null) {
            throw new AssertionError("Reference category not loaded for integration test: " + categoryName);
        }
        return category;
    }
}
