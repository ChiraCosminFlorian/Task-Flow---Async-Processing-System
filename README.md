# TaskFlow ⚡

[![CI](https://github.com/ChiraCosminFlorian/Task-Flow---Async-Processing-System/actions/workflows/ci.yml/badge.svg)](https://github.com/ChiraCosminFlorian/Task-Flow---Async-Processing-System/actions/workflows/ci.yml)
![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.4-brightgreen?logo=springboot)
![RabbitMQ](https://img.shields.io/badge/RabbitMQ-3-ff6600?logo=rabbitmq)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-336791?logo=postgresql)
![Docker](https://img.shields.io/badge/Docker-ready-2496ED?logo=docker)

Asynchronous task processing system built with Spring Boot, RabbitMQ, Spring Batch and PostgreSQL. Demonstrates message queuing, background job execution, CSV batch import and audit logging — fully containerized with Docker Compose and CI via GitHub Actions.

---

## Features

- ⚡ Async processing via RabbitMQ with direct exchange and routing
- 📦 Spring Batch CSV import with chunk-oriented processing
- 🔄 Job lifecycle tracking: `PENDING` → `PROCESSING` → `COMPLETED` / `FAILED`
- 📝 Audit log for every status change
- 🔁 Retry mechanism (max 3 attempts) + Dead-letter queue (`taskflow.dlq`)
- 📖 REST API with Swagger UI at `/swagger-ui.html`
- 🧪 Integration tests with Testcontainers and Awaitility
- 🐳 Docker Compose — one command to run the full stack
- ✅ GitHub Actions CI with automated testing and Docker image push

---

## Tech Stack

| Layer | Technology |
|-------|------------|
| Language | Java 21 |
| Framework | Spring Boot 3.4.4 |
| Messaging | RabbitMQ + Spring AMQP |
| Batch | Spring Batch |
| Database | PostgreSQL 16 + Spring Data JPA |
| API Docs | Springdoc OpenAPI (Swagger) |
| Testing | JUnit 5, Mockito, Testcontainers, Awaitility |
| Build | Maven |
| Containerization | Docker + Docker Compose |
| CI | GitHub Actions |

---

## Getting Started

### Prerequisites

- Java 21+
- Docker & Docker Compose

### Run with Docker Compose

```bash
git clone https://github.com/ChiraCosminFlorian/Task-Flow---Async-Processing-System.git
cd Task-Flow---Async-Processing-System
docker-compose up --build
```

The application will be available at:
- **API:** http://localhost:8080/api/tasks
- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **RabbitMQ Management:** http://localhost:15672

---

## API Reference

### Create a Task

```http
POST /api/tasks
Content-Type: application/json

{
  "taskType": "EMAIL",
  "payload": "{\"to\":\"user@example.com\",\"subject\":\"Welcome\"}"
}
```

**Response** `201 Created`:
```json
{
  "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "taskType": "EMAIL",
  "status": "PENDING",
  "payload": "{\"to\":\"user@example.com\",\"subject\":\"Welcome\"}",
  "createdAt": "2026-03-25T22:00:00",
  "updatedAt": "2026-03-25T22:00:00",
  "retryCount": 0,
  "errorMessage": null
}
```

### Get Task by ID

```http
GET /api/tasks/{id}
```

### List Tasks (with optional status filter)

```http
GET /api/tasks?status=FAILED
```

### Get Aggregated Statistics

```http
GET /api/tasks/stats
```

**Response** `200 OK`:
```json
{
  "totalJobs": 120,
  "pendingJobs": 15,
  "completedJobs": 95,
  "failedJobs": 10
}
```

### Retry a Failed Task

```http
POST /api/tasks/{id}/retry
```

### Import Tasks from CSV

```http
POST /api/batch/import
Content-Type: multipart/form-data

file: tasks.csv
```

**CSV format:**
```csv
taskType,payload
EMAIL,{"to":"user@example.com","subject":"Hello"}
REPORT,{"format":"PDF","period":"monthly"}
CSV_IMPORT,{"source":"products.csv"}
```

### Get Batch Job Status

```http
GET /api/batch/status/{jobId}
```

---

## Architecture

### Main Flow

```
┌──────────┐     ┌───────────┐     ┌──────────────┐     ┌────────────┐
│ REST API │────▶│ RabbitMQ  │────▶│ Async Worker │────▶│ PostgreSQL │
│ (POST)   │     │  (Queue)  │     │ (Consumer)   │     │   (Save)   │
└──────────┘     └───────────┘     └──────────────┘     └────────────┘
                       │                   │
                       ▼                   ▼
                 ┌───────────┐     ┌──────────────┐
                 │    DLQ    │     │  Audit Log   │
                 │ (Failed)  │     │  (History)   │
                 └───────────┘     └──────────────┘
```

### Task Lifecycle

```
                    ┌─────────┐
                    │ PENDING │
                    └────┬────┘
                         │
                         ▼
                  ┌────────────┐
                  │ PROCESSING │
                  └──────┬─────┘
                    ┌────┴────┐
                    │         │
                    ▼         ▼
             ┌───────────┐ ┌────────┐
             │ COMPLETED │ │ FAILED │
             └───────────┘ └───┬────┘
                               │
                     retryCount < 3?
                      ┌────┴────┐
                      │ YES     │ NO
                      ▼         ▼
               ┌─────────┐  ┌──────┐
               │ PENDING  │  │ DLQ  │
               │ (retry)  │  │      │
               └──────────┘  └──────┘
```

---

## Running Tests

```bash
# All tests (unit + integration) — requires Docker
mvn verify

# Unit tests only
mvn test

# Skip tests (build only)
mvn package -DskipTests
```

### Test Layers

| Layer | Framework | Description |
|-------|-----------|-------------|
| Unit | Mockito + JUnit 5 | Service logic in isolation |
| Slice | `@WebMvcTest` | Controller layer with mocked services |
| Integration | Testcontainers + Awaitility | Full async flow with real PostgreSQL & RabbitMQ |

---

## Project Structure

```
src/main/java/com/taskflow/
├── config/        # AppConfig, RabbitMQConfig, BatchConfig, JpaAuditingConfig, OpenApiConfig
├── controller/    # TaskController, BatchController, GlobalExceptionHandler
├── service/       # TaskService, EmailTaskHandler, ReportTaskHandler, CsvImportTaskHandler
├── batch/         # CsvImportJobConfig, CsvTaskItemReader, Processor, Writer
├── messaging/     # TaskMessageConsumer (RabbitMQ listener with retry logic)
├── model/         # JobTask, AuditLog, TaskType, TaskStatus
├── repository/    # JobTaskRepository, AuditLogRepository
└── dto/           # CreateTaskRequest, TaskResponse, TaskStatusResponse, CsvTaskRecord
```

---

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_HOST` | `localhost` | PostgreSQL host |
| `DB_PORT` | `5432` | PostgreSQL port |
| `DB_NAME` | `taskflow` | Database name |
| `DB_USERNAME` | `postgres` | Database username |
| `DB_PASSWORD` | `postgres` | Database password |
| `RABBITMQ_HOST` | `localhost` | RabbitMQ host |
| `RABBITMQ_PORT` | `5672` | RabbitMQ AMQP port |

---

## RabbitMQ Management UI

- **URL:** http://localhost:15672
- **Credentials:** `guest` / `guest`

**Queues:**

| Queue | Purpose |
|-------|---------|
| `taskflow.queue` | Main processing queue |
| `taskflow.dlq` | Dead-letter queue for failed tasks (after max retries) |

---

## License

This project is licensed under the [MIT License](LICENSE).
