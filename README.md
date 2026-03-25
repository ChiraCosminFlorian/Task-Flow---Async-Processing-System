# TaskFlow — Async Processing System

[![TaskFlow CI](https://github.com/ChiraCosminFlorian/Task-Flow---Async-Processing-System/actions/workflows/ci.yml/badge.svg)](https://github.com/ChiraCosminFlorian/Task-Flow---Async-Processing-System/actions/workflows/ci.yml)

Asynchronous task processing system built with Spring Boot, RabbitMQ, Spring Batch and PostgreSQL. Demonstrates message queuing, background job execution, CSV batch import and audit logging — fully containerized with Docker Compose and CI via GitHub Actions.

## Tech Stack

- **Java 21** / **Spring Boot 3.4**
- **PostgreSQL 16** — persistence
- **RabbitMQ 3** — async messaging with dead-letter queue
- **Spring Batch** — CSV bulk import
- **Swagger UI** — API documentation
- **Testcontainers + Awaitility** — integration testing
- **Docker + Docker Compose** — containerized deployment

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/tasks` | Create a new task |
| `GET` | `/api/tasks` | List tasks (optional `?status=` filter) |
| `GET` | `/api/tasks/{id}` | Get task by ID |
| `GET` | `/api/tasks/stats` | Aggregated statistics |
| `POST` | `/api/tasks/{id}/retry` | Retry a FAILED task |
| `POST` | `/api/batch/import` | Import tasks from CSV file |
| `GET` | `/api/batch/status/{jobId}` | Batch job execution status |

## Quick Start

```bash
# Start all services
docker-compose up --build

# Swagger UI
open http://localhost:8080/swagger-ui.html

# RabbitMQ Management
open http://localhost:15672  # guest/guest
```

## Project Structure

```
com.taskflow
├── config/       # App, RabbitMQ, JPA, Batch, OpenAPI configs
├── controller/   # REST controllers (Task, Batch)
├── service/      # Business logic + task handlers
├── batch/        # Spring Batch CSV import job
├── messaging/    # RabbitMQ consumer with retry logic
├── model/        # JPA entities (JobTask, AuditLog)
├── repository/   # Spring Data repositories
└── dto/          # Request/response records
```

## Running Tests

```bash
# Requires Docker (for Testcontainers)
mvn verify
```
