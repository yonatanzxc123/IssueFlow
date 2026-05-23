# IssueFlow - Implementation Notes

This project implements the IssueFlow Ticket Management Backend Platform according to the provided README and assignment PDF.

The backend is implemented with:

- Java 21
- Spring Boot
- Spring Security with JWT authentication
- Spring Data JPA / Hibernate
- PostgreSQL
- Maven Wrapper
- Docker Compose for local PostgreSQL

## Implemented Features

The following major features are implemented:

- Users API
- JWT Authentication API
- Projects API
- Tickets API
- Soft delete and restore for projects and tickets
- Comments API
- `@username` mentions in comments
- Ticket dependency/blocker API
- Auto-assignment for tickets without an explicit assignee
- Project workload endpoint
- CSV ticket import/export
- Attachment upload/list/delete
- Automatic overdue ticket escalation scheduler
- Read-only Audit Log API
- Global error handling and validation
- Automated integration/regression tests
- Manual Postman validation collections

## Authentication Notes

All endpoints are JWT-protected except:

- `POST /users`
- `POST /auth/login`

Login returns a Bearer JWT token. Protected endpoints require:

Authorization: Bearer <token>