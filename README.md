# Codzee Java Code Review Demo

A small Task Management REST API created as an independent baseline for the Codzee Senior Java Developer interview assessment. The repository is structured so business validation, API error behavior, and ownership/security improvements can be reviewed in separate future pull requests.

## Technology

- Java 17
- Spring Boot 3.5.16
- Maven
- Spring Web, Validation, and Data JPA
- H2 in-memory database
- JUnit 5, Mockito, Spring Boot Test, and MockMvc

## Prerequisites

- JDK 17 installed and available as `java`
- Maven 3.6.3 or newer available as `mvn`

Check versions with `java -version` and `mvn -version`.

## Build and run

From the repository root:

```powershell
mvn clean verify
mvn spring-boot:run
```

The API listens on `http://localhost:8080`.

## API

All task responses are DTOs and include `id`, `title`, `description`, `status`, `priority`, `createdAt`, and `updatedAt`. Status values are `TODO`, `IN_PROGRESS`, and `DONE`; priority values are `LOW`, `MEDIUM`, and `HIGH`.

| Method | Path | Behavior |
| --- | --- | --- |
| POST | `/api/tasks` | Create a task; returns `201 Created` |
| GET | `/api/tasks/{id}` | Retrieve a task |
| GET | `/api/tasks?page=0&size=20&status=TODO` | List tasks, optionally filtering by status |
| PUT | `/api/tasks/{id}` | Replace task fields |
| DELETE | `/api/tasks/{id}` | Delete a task; returns `204 No Content` |

Pagination is zero-based, defaults to page 0 and size 20, and accepts sizes from 1 through 100. Invalid payloads, enum values, and pagination arguments return a structured JSON error.

Example create request:

```http
POST /api/tasks
Content-Type: application/json

{
  "title": "Review pull request",
  "description": "Check the service and API tests",
  "status": "TODO",
  "priority": "HIGH"
}
```

Example list request: `GET /api/tasks?page=0&size=20&status=TODO`.

## Database

The default profile uses an H2 in-memory database named `tasks`. Data is cleared when the application stops. Hibernate creates or updates the schema for local use. The H2 console is disabled. Datasource settings can be overridden with standard Spring Boot environment variables or properties.

## Tests

Run `mvn test` for unit and MockMvc integration tests, or `mvn clean verify` for the full Maven verification lifecycle. Tests use H2 and do not call external services.

## Current limitations

- Authentication, authorization, and task ownership are intentionally absent from this baseline; they are reserved for a separately reviewed security/data-integrity change.
- H2 is intended for local development and tests, not production persistence.
- The API uses offset pagination and does not provide sorting parameters.
- Schema management uses Hibernate `update`; a production deployment should use a migration tool and an explicitly managed database.
