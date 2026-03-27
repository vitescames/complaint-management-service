# complaint-management-service

## Overview

`complaint-management-service` is a production-style PoC built with Java 17 and Spring Boot 4.
It receives complaints through REST and JMS, classifies them with persisted category keywords, stores them in H2 through JPA, and publishes asynchronous notifications through an embedded ActiveMQ broker.

The project is intentionally structured for a senior-level technical case:

- Hexagonal architecture with strict layer boundaries
- Domain-centric design with always-valid domain objects
- Separate input DTOs and mappers per channel
- Lightweight CQRS with commands and queries
- Embedded infrastructure for local execution
- Observer-based domain event flow
- Resilience around infrastructure-facing adapters
- Full Maven build verification with JaCoCo at 100%

## Architecture

The codebase is organized around these macro layers:

- `domain`
  - Pure business model and business rules
  - Value objects such as `Cpf`, `EmailAddress`, `ComplaintText`, and `DocumentUrl`
  - Domain services such as complaint classification and SLA policy
  - Domain events
- `application`
  - Use cases
  - Commands and queries
  - Ports in and ports out
  - Observer-style domain event publisher
- `adapters.in`
  - REST controller, HTTP error models, and request/response DTOs
  - JMS listener and queue DTOs
  - Channel-specific mappers that normalize payloads into the same application flow
- `adapters.out`
  - Persistence implementation with JPA entities and Spring Data repositories
  - Messaging publishers for queue notifications
- `root package`
  - Application bean wiring
- `adapters.out.config`
  - Embedded ActiveMQ setup
  - Configuration properties
- `adapters.out.resilience`
  - Resilience support

### High-level flow

```mermaid
flowchart LR
    A["POST /complaints"] --> C["CreateComplaintCommand"]
    B["complaint.received.queue"] --> C["CreateComplaintCommand"]
    C --> D["CreateComplaintUseCaseImpl"]
    D --> E["ComplaintCategoryClassifier"]
    D --> F["ComplaintRepositoryPort"]
    D --> G["ObserverDomainEventPublisher"]
    G --> H["ComplaintCreatedQueueObserver"]
    H --> I["complaint.created.queue"]
    J["Daily SLA scheduler"] --> K["PublishSlaWarningsUseCaseImpl"]
    K --> G
    G --> L["ComplaintSlaWarningQueueObserver"]
    L --> M["complaint.sla.warning.queue"]
```

## Technical Decisions

- The domain does not depend on Spring, JPA, JMS, or Bean Validation.
- REST and queue payloads are intentionally different and normalized by dedicated mappers into the same `CreateComplaintCommand`.
- Commands and queries use manual builders and keep transport-neutral data. They remain lightweight carriers, and domain objects are created inside the use cases.
- Complaint classification is data-driven. Categories and keywords are loaded from the database, so new categories can be introduced without changing classifier code.
- Complaint status is modeled as a domain enum and also backed by a reference table with fixed ids:
  - `1 = PENDING`
  - `2 = PROCESSING`
  - `3 = RESOLVED`
- The initial complaint status is always `PENDING` and is never chosen by the client.
- The HTTP layer returns explicit error response models instead of exposing raw `ProblemDetail`.
- Resilience is applied at the outgoing adapter boundary, mainly around persistence and queue publishing, so business rules stay clean.
- ActiveMQ runs embedded inside the application with dead-letter handling configured through destination policy.
- Flyway is the only mechanism used to create and seed the database schema.

## Technology Stack

- Java 17
- Spring Boot 4.0.4
- Maven
- Spring Web
- Spring Data JPA
- Bean Validation
- H2 embedded database
- Flyway
- JMS with embedded ActiveMQ
- Resilience4j
- JUnit 5
- Mockito
- JaCoCo

## Project Structure

```text
src/main/java/com/complaintmanagementservice
|-- adapters
|   |-- in
|   |   |-- messaging
|   |   |-- rest
|   |   `-- scheduler
|   `-- out
|       |-- config
|       |-- messaging
|       |-- persistence
|       `-- resilience
|-- ApplicationConfiguration.java
|-- application
`-- domain
```

## Running Locally

### Prerequisites

- Java 17
- Maven 3.9+

### Start the application

```bash
mvn spring-boot:run
```

By default the application runs on port `8080`.

If port `8080` is already in use, run:

```bash
mvn spring-boot:run "-Dspring-boot.run.arguments=--server.port=0"
```

### What starts automatically

When the application starts, it also brings up:

- H2 in-memory database
- Flyway migrations and reference data
- Embedded ActiveMQ broker
- JMS queues used by the application
- Daily scheduler

### Local URLs

- API base URL: `http://localhost:8080`
- H2 console: `http://localhost:8080/h2-console`

H2 connection values:

- JDBC URL: `jdbc:h2:mem:complaintsdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE`
- User: `sa`
- Password: empty

## Build, Tests, and Coverage

Run the complete build:

```bash
mvn clean verify
```

Run only the test suite:

```bash
mvn test
```

Open the JaCoCo report after the build:

- `target/site/jacoco/index.html`

The Maven build is configured to fail if coverage is below 100% for:

- instructions
- branches
- lines
- methods

## Flyway and Reference Data

Flyway migration:

- `src/main/resources/db/migration/V1__create_schema.sql`

It creates and seeds:

- `customers`
- `complaint_statuses`
- `categories`
- `category_keywords`
- `complaints`
- `complaint_categories`
- `complaint_documents`

Reference data includes:

- complaint statuses
- initial complaint categories
- initial category keywords

## REST API

### POST `/complaints`

Creates a complaint through the REST channel.

#### Request example

```json
{
  "customer": {
    "cpf": "52998224725",
    "name": "Maria Silva",
    "birthDate": "1990-05-10",
    "email": "maria.silva@example.com"
  },
  "complaintCreatedDate": "2026-03-20",
  "complaintText": "Não consigo acessar o app e a senha sempre falha",
  "documentUrls": [
    "https://example.com/documents/evidence-1.pdf",
    "https://example.com/documents/evidence-2.pdf"
  ]
}
```

#### cURL example

```bash
curl -i -X POST "http://localhost:8080/complaints" \
  -H "Content-Type: application/json" \
  -d '{
    "customer": {
      "cpf": "52998224725",
      "name": "Maria Silva",
      "birthDate": "1990-05-10",
      "email": "maria.silva@example.com"
    },
    "complaintCreatedDate": "2026-03-20",
    "complaintText": "Não consigo acessar o app e a senha sempre falha",
    "documentUrls": [
      "https://example.com/documents/evidence-1.pdf"
    ]
  }'
```

#### Success response example

```json
{
  "complaintId": "11111111-1111-1111-1111-111111111111",
  "statusId": 1,
  "statusName": "PENDING"
}
```

#### Error response examples

Validation and parsing errors return a field-oriented structure:

```json
{
  "title": "Dados inválidos",
  "status": 400,
  "errors": [
    {
      "field": "customer.email",
      "message": "Formato inválido"
    }
  ]
}
```

Business and generic failures return a simple message structure:

```json
{
  "title": "Regra de negócio violada",
  "status": 422,
  "message": "A data da reclamação não pode ser futura."
}
```

Expected behavior:

- returns `201 Created`
- sets the `Location` header to `/complaints/{complaintId}`
- persists the complaint
- classifies the complaint automatically
- publishes a complaint-created notification

### GET `/complaints`

Searches complaints ordered from newest complaint date to oldest.

Supported optional filters:

- `customerCpf`
- `categories`
- `status`
- `startDate`
- `endDate`

Date rules:

- only `startDate`: returns complaints from that date onward
- only `endDate`: returns complaints from the beginning up to that date
- both dates: returns the range
- no dates: returns everything

#### cURL example

```bash
curl "http://localhost:8080/complaints?customerCpf=52998224725&categories=acesso&categories=aplicativo&status=1&startDate=2026-03-01&endDate=2026-03-31"
```

#### Response example

```json
[
  {
    "complaintId": "11111111-1111-1111-1111-111111111111",
    "complaintCreatedDate": "2026-03-20",
    "complaintText": "Não consigo acessar o app e a senha sempre falha",
    "status": {
      "id": 1,
      "name": "PENDING"
    },
    "customer": {
      "cpf": "52998224725",
      "name": "Maria Silva",
      "birthDate": "1990-05-10",
      "email": "maria.silva@example.com"
    },
    "categories": [
      {
        "id": 4,
        "name": "acesso"
      },
      {
        "id": 5,
        "name": "aplicativo"
      }
    ],
    "documentUrls": [
      "https://example.com/documents/evidence-1.pdf"
    ],
    "registeredAt": "2026-03-23T00:00:00Z"
  }
]
```

## Messaging

### Configured queues

- `complaint.received.queue`
- `complaint.created.queue`
- `complaint.sla.warning.queue`

Dead-letter queues are configured with the `DLQ.` prefix by ActiveMQ policy. For example:

- `DLQ.complaint.received.queue`

### Inbound message format

The queue listener accepts a different payload than the REST API.

```json
{
  "customerDocument": "52998224725",
  "customerFullName": "Maria Silva",
  "customerBirthDate": "1990-05-10",
  "customerEmailAddress": "maria.silva@example.com",
  "occurrenceDate": "2026-03-20",
  "description": "Não consigo acessar o app e a senha sempre falha"
}
```

### Example of sending a message to the inbound queue

This PoC uses an embedded broker with VM transport, so queue publishing is intended to happen in-process.
The simplest way to send a message manually is through `JmsTemplate` inside the running application context:

```java
CreateComplaintQueueMessage payload = new CreateComplaintQueueMessage(
        "52998224725",
        "Maria Silva",
        LocalDate.of(1990, 5, 10),
        "maria.silva@example.com",
        LocalDate.of(2026, 3, 20),
        "Não consigo acessar o app e a senha sempre falha"
);

jmsTemplate.convertAndSend("complaint.received.queue", payload);
```

### Published queue messages

After a complaint is created successfully:

```json
{
  "complaintId": "11111111-1111-1111-1111-111111111111",
  "createdAt": "2026-03-23T00:00:00Z"
}
```

When the SLA warning job finds a complaint near the deadline:

```json
{
  "complaintId": "11111111-1111-1111-1111-111111111111",
  "slaDeadlineDate": "2026-03-30"
}
```

## Automatic Complaint Classification

Classification is based on complaint text and persisted category keywords.

Initial categories and examples of keywords:

- `imobiliario`: `credito imobiliario`, `casa`, `apartamento`
- `seguros`: `resgate`, `capitalizacao`, `socorro`
- `cobranca`: `fatura`, `cobranca`, `valor`, `indevido`
- `acesso`: `acessar`, `login`, `senha`
- `aplicativo`: `app`, `aplicativo`, `travando`, `erro`
- `fraude`: `fatura`, `nao reconhece divida`, `fraude`

How it works:

- the classifier normalizes the complaint text
- it compares the normalized text with the persisted keyword catalog
- it returns all matching categories
- it does not require code changes to introduce new categories or new keywords

Because categories are loaded from the database, evolving the catalog is a data change, not a source-code change.

## Domain Event Flow

Complaint creation and SLA warning publication use a classic observer flow:

1. A complaint is created in the domain and emits `ComplaintCreatedDomainEvent`.
2. The application saves the complaint.
3. The application publishes the domain event through `ObserverDomainEventPublisher`.
4. Registered observers are notified through the shared `DomainEventObserver` contract.
5. `ComplaintCreatedQueueObserver` reacts to `ComplaintCreatedDomainEvent`.
6. The messaging adapter publishes the final message to `complaint.created.queue`.

For SLA warnings:

1. The scheduler triggers `PublishSlaWarningsUseCaseImpl`.
2. The use case identifies complaints that entered the SLA warning window.
3. The use case publishes `ComplaintSlaWarningTriggeredDomainEvent`.
4. `ComplaintSlaWarningQueueObserver` reacts to that domain event.
5. The messaging adapter publishes the final message to `complaint.sla.warning.queue`.

This keeps the domain independent from ActiveMQ while still enabling asynchronous reactions after successful creation.

## SLA Warning Job

The scheduled job runs every day at `07:00` using:

- `application.scheduler.sla-warning-cron=0 0 7 * * *`

The rule is:

- consider complaints whose status is not `RESOLVED`
- calculate the SLA deadline as `complaint created date + 10 days`
- publish a warning when the complaint is exactly 3 days away from that deadline

In practice, the scheduler looks for complaints created 7 days before the reference day and publishes one message per matching complaint.

Duplicate warning publication is allowed in this PoC.

## Validation Strategy

Validation is intentionally split into three levels:

1. Edge validation
   - Bean Validation on REST DTOs and queue DTOs
2. Application validation
   - input normalization in channel mappers when it improves request clarity
   - semantic checks in use cases, such as date range validation and future complaint date rejection
3. Domain validation
   - always-valid entities and value objects

This keeps invalid state out of the system as early as possible while still protecting the core domain.
Commands and queries remain lightweight and do not instantiate domain objects by themselves.

## Resilience Strategy

Resilience is applied to outbound infrastructure-facing operations:

- persistence adapter
- queue publisher adapter

The goals are:

- fail fast
- reduce pressure on degraded infrastructure
- keep retry logic out of controllers and domain objects

Outbound adapter failures are not wrapped in artificial exceptions. Anything that is not mapped explicitly in the HTTP layer falls back to a friendly `500` response.

Profiles are configured in `src/main/resources/application.yml` for:

- `application.resilience.persistence`
- `application.resilience.messaging`

## Main Files

- Application entry point:
  - `src/main/java/com/complaintmanagementservice/ComplaintManagementServiceApplication.java`
- REST controller:
  - `src/main/java/com/complaintmanagementservice/adapters/in/rest/ComplaintController.java`
- REST exception handler:
  - `src/main/java/com/complaintmanagementservice/adapters/in/rest/ApiExceptionHandler.java`
- Queue listener:
  - `src/main/java/com/complaintmanagementservice/adapters/in/messaging/ComplaintReceivedListener.java`
- Complaint creation use case:
  - `src/main/java/com/complaintmanagementservice/application/usecase/CreateComplaintUseCaseImpl.java`
- Complaint search use case:
  - `src/main/java/com/complaintmanagementservice/application/usecase/SearchComplaintsUseCaseImpl.java`
- SLA warning use case:
  - `src/main/java/com/complaintmanagementservice/application/usecase/PublishSlaWarningsUseCaseImpl.java`
- Domain classifier:
  - `src/main/java/com/complaintmanagementservice/domain/service/ComplaintCategoryClassifier.java`
- SLA policy:
  - `src/main/java/com/complaintmanagementservice/domain/service/ComplaintSlaPolicy.java`
- SLA warning domain event:
  - `src/main/java/com/complaintmanagementservice/domain/event/ComplaintSlaWarningTriggeredDomainEvent.java`
- Scheduler:
  - `src/main/java/com/complaintmanagementservice/adapters/in/scheduler/SlaWarningScheduler.java`
- Messaging configuration:
  - `src/main/java/com/complaintmanagementservice/adapters/out/config/MessagingConfiguration.java`
- Application configuration:
  - `src/main/java/com/complaintmanagementservice/ApplicationConfiguration.java`
- Flyway migration:
  - `src/main/resources/db/migration/V1__create_schema.sql`

## Notes

- The project uses `application.yml` only.
- There is no update or delete flow by design.
- Pagination is intentionally out of scope for this PoC.
- The embedded broker is configured for local execution and technical presentation.
