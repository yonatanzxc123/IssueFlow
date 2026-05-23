# IssueFlow

IssueFlow is a Java 21 Spring Boot backend for a ticket-management platform

This repository contains the backend implementation, automated tests, run instructions, implementation notes, and manual Postman validation artifacts.

## Where to Start

| File / Folder | What it contains |
|--------------|------------------|
| `run.md` | How to run the project, start PostgreSQL, run tests, and use the Postman collections |
| `Instructions.md` | Important implementation notes, assumptions, and compatibility decisions |
| `manual-tests/README.md` | Documentation of the manual Postman validation performed |
| `manual-tests/postman/` | Postman collections used for manual testing |
| `manual-tests/files/` | Helper files for CSV import and attachment tests |
| `prompts.md` | Summary of the AI-assisted planning/implementation workflow |
| `src/` | Main source code and automated tests |
| `compose.yml` | Local PostgreSQL setup for Docker Compose |
| `pom.xml` | Maven project configuration |

## Quick Run

```powershell
docker compose up -d
.\mvnw.cmd spring-boot:run
