
## Table of Contents
| Prompt | Task                                      | 
|--------|-------------------------------------------| 
| 1      | Project inspection only                   |
| 2      | Foundation entities and enums             | 
| 3      | DTOs, mappers, validation, error handling | 
| 4      | Users and JWT authentication              | 
| 5      | Projects API                              |
| 6      | Tickets API core                          |
| 7      | Comments and mentions                     | 
| 8      | Ticket dependencies                       | 
| 9      | Auto-assignment and workload              | 
| 10     | CSV import/export                         | 
| 11     | Attachments                               |
| 12     | Auto-escalation scheduler                 | 
| 13     | Audit Log API                             |
| 14     | Final tests/ regression tests             |



Note : I used for the Main prompt codex with GPT-5.5 and for advice/minor fixes I used Gemeni 3.1 pro and ChatGpt model 5.5


Main Prompts:

Prompt 1 - Project inspection:


You are helping me implement a Java 21 Spring Boot backend home assignment called IssueFlow.

First, inspect the existing repository structure, README.md, Instructions.md, pom.xml, compose.yml, schema.sql, data.sql, and application.yaml.

Do not modify code yet.

Return:
1. The current project structure.
2. The Spring Boot version and important dependencies.
3. The existing database configuration.
4. The existing schema/data assumptions.
5. A proposed package structure under com.att.tdp.issueflow.
6. A phased implementation plan that follows README.md exactly as the API contract.

Constraints:
- Do not introduce Kafka, RabbitMQ, microservices, or unnecessary infrastructure.
- Prefer simple Spring Boot layered architecture.
- Prefer PostgreSQL + Spring Data JPA.
- The solution must remain understandable to a junior developer.








Prompt 2 — Foundation entities and enums:

Implement the foundation model for IssueFlow.

Before editing, inspect README.md, schema.sql, data.sql, pom.xml, and application.yaml.

Create or update the following:
- Enums: Role, TicketStatus, TicketPriority, TicketType, ActorType, AuditAction, AuditEntityType.
- Entities: User, Project, Ticket, Comment, AuditLog, TicketDependency, Attachment, Mention, RevokedToken if needed.
- Repositories for each persistent entity.

Use JPA/Hibernate annotations.
Use PostgreSQL-friendly table names.
Add createdAt/updatedAt where appropriate.
Add deleted/deletedAt for Project and Ticket.
Add @Version to Ticket and Comment for optimistic locking.

Important decisions:
- Do not introduce Kafka, RabbitMQ, microservices, or external infrastructure.
- Do not introduce a ProjectMember table unless README.md explicitly defines one.
- For later auto-assignment, assume all users with role DEVELOPER are candidates, and workload is counted by non-DONE tickets assigned to that developer within the same project.
- ADMIN users must not be candidates for auto-assignment later.
- Controllers should eventually return DTOs, so avoid relying on entity JSON serialization.
- Keep code simple and understandable for a junior developer.

Database initialization:
- If schema.sql/data.sql contain placeholder task/demo tables that conflict with JPA startup, neutralize them safely.
- Do not create unrelated task tables.
- Prefer letting JPA manage the schema during development using ddl-auto=update.

Constraints:
- Do not implement controllers yet.
- Do not implement services yet except minimal shared entity support if required.
- Do not implement JWT/security yet.
- Do not implement business logic yet.
- Keep fields aligned with README.md request/response bodies and the assignment requirements.
- Avoid circular JSON serialization by planning to use DTOs later, not entity responses.
- Make the code compile.

After changes, tell me exactly:
1. Which files were created or changed.
2. Any assumptions you made.
3. What command I should run to verify the build.






Prompt 3 — DTOs, mappers, and error handling:
Add DTOs, mappers, validation, formatting cleanup, and global error handling for IssueFlow.

Before editing, inspect:
- README.md
- Instructions.md
- existing domain entities
- existing repositories
- application.yaml
- pom.xml

Implement:
1. Request/response DTOs for:
  - users
  - projects
  - tickets
  - comments
  - auth login/token/me responses if useful for the next phase
  - audit log response if simple
  - dependency response if simple
  - attachment metadata response if simple
  - mentioned user/comment response if simple

2. Mapper classes or simple mapper methods for:
  - User -> UserResponse
  - Project -> ProjectResponse
  - Ticket -> TicketResponse
  - Comment -> CommentResponse
  - AuditLog -> AuditLogResponse if created
  - Attachment -> AttachmentResponse if created

3. Bean validation annotations:
  - @NotBlank for required strings
  - @Email for email
  - @NotNull for required IDs/enums
  - @Size where reasonable
  - Do not overcomplicate validation

4. Global exception handling:
  - Use @RestControllerAdvice
  - Create custom exceptions:
    ResourceNotFoundException
    BadRequestException
    ConflictException
    UnauthorizedException if useful
  - Create a consistent error response body:
    timestamp
    status
    error
    message
    path

5. Validation error handling:
  - Handle MethodArgumentNotValidException
  - Return informative validation messages
  - Do not expose stack traces to API clients

Constraints:
- Do not implement controllers yet.
- Do not implement services yet.
- Do not implement JWT/security yet.
- Do not implement business logic yet.
- Controllers later must return DTOs, never entities.
- Keep DTOs aligned with README.md request/response bodies and the assignment requirements.
- Keep the code simple and readable for a junior developer.
- Do not introduce Kafka, RabbitMQ, microservices, or external infrastructure.
- Do not add unnecessary dependencies unless required.
- Make the code compile.

After changes, tell me:
1. Which files were created or changed.
2. Whether any existing Java files were reformatted.
3. Any assumptions made.
4. The exact command I should run to verify.






Prompt 4 — Users and authentication:
Implement Users API and JWT Authentication API according to README.md and the assignment requirements PDF.

Before editing, inspect:
- README.md
- pom.xml
- application.yaml
- User entity
- UserRepository
- RevokedToken entity/repository
- existing DTOs, mappers, and exceptions
- existing GlobalExceptionHandler

Important contract notes:
- The PDF says README.md is the API implementation contract.
- README.md shows POST /users with exactly: username, email, fullName, role.
- README.md shows POST /auth/login with: username, password.
- Therefore, keep POST /users compatible with the README body that has no password field.
- Do NOT make password mandatory for POST /users.
- You may add an optional password field to CreateUserRequest.
- If password is provided, hash and store that password.
- If password is missing or blank, assign the default development password "secret".
- Add a short code comment explaining that the default password exists only to preserve compatibility with the provided README API contract.
- Store only a BCrypt password hash in User.passwordHash.
- Never return passwordHash in any response.

Endpoints to implement exactly:
- GET /users
- GET /users/{userId}
- POST /users
- POST /users/update/{userId}
- DELETE /users/{userId}
- POST /auth/login
- POST /auth/logout
- GET /auth/me

Authentication/security requirements:
- - The PDF states: "The system must protect all API endpoints using JWT-based authentication." Implement this strictly: every API endpoint must require a valid non-revoked JWT, except POST /users and POST /auth/login, which must remain public so users can register and obtain a token.

- Use JWT Bearer authentication.
- Add Spring Security and JWT dependencies to pom.xml if missing.
- Use stateless Spring Security session management.
- Disable CSRF for this stateless REST API.

- POST /auth/login must validate username and password and return a signed JWT.
- POST /auth/logout must invalidate the current Bearer token.
- Implement logout using the existing RevokedToken entity/repository as a persisted token deny-list.
- JWT authentication must reject revoked tokens.
- GET /auth/me must return the currently authenticated user profile.
- Use BCryptPasswordEncoder for password hashing and verification.

Endpoint behavior:
follow the READme.md
- GET /users returns List<UserResponse>.
- GET /users/{userId} returns UserResponse.
- POST /users returns UserResponse with 200 OK, matching README.
- POST /users/update/{userId} updates only fullName and role, and returns 200 OK with no response body.
- DELETE /users/{userId} deletes the user and returns 200 OK with no response body.
- POST /auth/login returns AuthTokenResponse with:
  - accessToken
  - tokenType = "Bearer"
  - expiresIn = 3600 or the configured expiration seconds
- POST /auth/logout returns 200 OK with no response body.
- GET /auth/me returns the current authenticated user profile. If CurrentUserResponse already exists and wraps UserResponse, using it is acceptable, but keep the response simple and compatible with README expectations.

Validation and errors:
- Role must be ADMIN or DEVELOPER.
- Duplicate username or email should return ConflictException.
- Missing user should return ResourceNotFoundException.
- Bad credentials should return 401 Unauthorized.
- Missing/invalid/expired/revoked JWT should return 401 Unauthorized.
- Use the existing DTOs, mappers, and GlobalExceptionHandler.
- Do not expose stack traces.
- Do not return passwordHash.

Implementation expectations:
- Create UserService for user management.
- Create AuthService for login/logout/current user.
- Create UserController.
- Create AuthController.
- Create SecurityConfig.
- Create JwtService or JwtTokenProvider.
- Create JwtAuthenticationFilter.
- If useful, create a custom UserDetailsService implementation.
- Use constructor injection.
- Keep code simple and readable for a junior developer.
- Make the code compile and tests pass.

Configuration:
- Add JWT secret and expiration settings to application.yaml.
- Use a development-safe default secret for local use only.
- Prefer allowing the secret to be overridden by environment variable.
- Example idea: app.security.jwt.secret with ${JWT_SECRET:...default...}
- Example idea: app.security.jwt.expiration-seconds=3600

Tests:
- If reasonable within this task, add basic tests for:
  - creating a user without password succeeds and stores a BCrypt hash for default password "secret"
  - creating a user with explicit password succeeds and stores a BCrypt hash
  - user responses never include passwordHash
  - duplicate username or email returns conflict
  - login with valid credentials returns JWT
  - login with invalid credentials fails
  - protected endpoint rejects missing token
  - revoked token is rejected after logout
- Do not over-focus on tests yet; full business-rule tests will come later.

Constraints:
- Do not implement projects/tickets/comments yet.
- Do not implement audit logging yet unless only a minimal placeholder is required.
- Do not add Kafka, RabbitMQ, microservices, or external infrastructure.
- Do not expose entities directly from controllers.
- Controllers must return DTOs.
- Avoid over-engineering.
- Keep public endpoint paths and response statuses aligned with README.md.

After changes, tell me:
1. Which files were created or changed.
2. Which dependencies were added.
3. How JWT secret/expiration is configured.
4. Which endpoints are public and which are protected.
5. How README compatibility for POST /users without password is handled.
6. Any assumptions made.
7. The exact command I should run to verify.








Prompt 5 — Projects API:

Implement Projects API according to README.md.

Before editing, inspect:
- README.md
- Project entity
- ProjectRepository
- User entity/UserRepository/UserService
- AuditLog entity/repository/enums
- existing DTOs and mappers
- existing exceptions and GlobalExceptionHandler
- existing SecurityConfig/UserPrincipal

Endpoints to implement:
- GET /projects
- GET /projects/{projectId}
- POST /projects
- PATCH /projects/{projectId}
- DELETE /projects/{projectId}
- GET /projects/deleted
- POST /projects/{projectId}/restore

Rules:
- Project has name, description, and ownerId.
- ownerId must reference an existing user.
- DELETE is soft delete only: set deleted=true and deletedAt=now.
- Normal GET endpoints must hide soft-deleted projects.
- GET /projects/deleted lists only soft-deleted projects.
- POST /projects/{projectId}/restore restores a soft-deleted project.
- Deleted listing and restore are ADMIN-only.
- All project endpoints must remain protected by JWT because SecurityConfig protects all endpoints except POST /users and POST /auth/login.
- Use the authenticated user from SecurityContext when audit logging needs an actor.

Audit logging:
- Create a simple AuditLogService if one does not exist.
- Record state-changing project actions:
    - CREATE_PROJECT
    - UPDATE_PROJECT
    - DELETE_PROJECT
    - RESTORE_PROJECT
- Use AuditLog entity/repository/enums already created.
- Actor should be the authenticated user when available.
- Keep audit details simple; do not over-engineer JSON diffs.

Implementation expectations:
- Create ProjectService.
- Create ProjectController.
- Use existing ProjectMapper/DTOs where possible.
- Controllers must return DTOs, not entities.
- Use constructor injection.
- Keep code simple and readable.
- Make the code compile and tests pass.

Endpoint behavior:
- GET /projects returns List<ProjectResponse> for non-deleted projects.
- GET /projects/{projectId} returns ProjectResponse only if not deleted.
- POST /projects returns ProjectResponse with 200 OK unless README clearly says another status.
- PATCH /projects/{projectId} updates name and/or description and returns ProjectResponse or 200 OK according to existing DTO/API style.
- DELETE /projects/{projectId} returns 200 OK with no response body.
- GET /projects/deleted returns List<ProjectResponse> and requires ADMIN.
- POST /projects/{projectId}/restore returns ProjectResponse or 200 OK according to existing style and requires ADMIN.

Validation/errors:
- Missing project returns ResourceNotFoundException.
- Missing owner user returns ResourceNotFoundException.
- Accessing deleted project through normal GET should behave as not found.
- Non-ADMIN access to deleted/restore endpoints should return 403 Forbidden.
- Invalid request bodies should use the existing validation error response.

Tests:
- Add focused tests if reasonable:
    - create project with valid owner succeeds
    - create project with missing owner fails
    - normal GET hides deleted project
    - deleted endpoint requires ADMIN
    - restore endpoint requires ADMIN
    - project state-changing actions write audit log
- Do not over-focus on tests yet; full business-rule tests will come later.

Constraints:
- Do not implement tickets/comments/dependencies/attachments/CSV/escalation yet.
- Do not add Kafka, RabbitMQ, microservices, or external infrastructure.
- Do not expose entities directly.
- Do not weaken JWT security.
- Keep endpoint paths aligned with README.md.

After changes, tell me:
1. Which files were created or changed.
2. Whether AuditLogService was created.
3. Which project actions are audited.
4. How ADMIN-only access is enforced.
5. Any assumptions made.
6. The exact command I should run to verify.






Prompt 6 — Tickets API core:
Implement ONLY Tickets API core according to README.md and the assignment requirements.

Hard scope limit:
Do NOT implement comments.
Do NOT implement mentions.
Do NOT implement ticket dependencies.
Do NOT implement attachments.
Do NOT implement CSV import/export.
Do NOT implement workload auto-assignment.
Do NOT implement scheduled escalation.
Do NOT implement audit-log querying.
If you need any of those, stop and explain why instead of implementing them.

Before editing, inspect:
- README.md
- Instructions.md
- Ticket entity
- TicketRepository
- Project entity / ProjectRepository / ProjectService
- User entity / UserRepository / UserService
- AuditLogService and AuditAction enum
- existing DTOs and mappers
- existing exceptions and GlobalExceptionHandler
- existing SecurityConfig / UserPrincipal

Contract notes:
- README.md is the main API contract.
- README.md defines:
    - GET /tickets?projectId=:projectId
    - GET /tickets/:ticketId
    - POST /tickets
    - PATCH /tickets/:ticketId
    - DELETE /tickets/:ticketId
- The PDF additionally requires:
    - GET /tickets/deleted?projectId={id}
    - POST /tickets/{id}/restore
    - soft-deleted tickets hidden from standard API responses
    - deleted/restore endpoints ADMIN-only
- The PDF says ticket create/update accept optional dueDate.
- Store and update dueDate, but do NOT implement auto-escalation in this prompt.
- TicketRepository may already contain helper methods for later workload/escalation; do not use those future features in this prompt.

Endpoints to implement:
- GET /tickets?projectId={projectId}
- GET /tickets/{ticketId}
- POST /tickets
- PATCH /tickets/{ticketId}
- DELETE /tickets/{ticketId}
- GET /tickets/deleted?projectId={projectId}
- POST /tickets/{ticketId}/restore

Security:
- All ticket endpoints must remain protected by JWT.
- GET /tickets/deleted and POST /tickets/{ticketId}/restore must be ADMIN-only.
- Use @PreAuthorize("hasRole('ADMIN')") consistently with the existing Projects API.
- Do not weaken existing JWT/security behavior.

Core ticket rules:
- A ticket belongs to exactly one project.
- projectId must reference an existing non-deleted project.
- assigneeId is optional.
- If assigneeId is provided, it must reference an existing user.
- Status must be one of: TODO, IN_PROGRESS, IN_REVIEW, DONE.
- Priority must be one of: LOW, MEDIUM, HIGH, CRITICAL.
- Type must be one of: BUG, FEATURE, TECHNICAL.
- A ticket cannot be updated once its current status is DONE.
- Status may only move forward in this lifecycle:
  TODO -> IN_PROGRESS -> IN_REVIEW -> DONE.
- Backward transitions are not allowed.
- Forward jumps are allowed if they move forward in the lifecycle.
- DELETE is soft delete only: set deleted=true and deletedAt=now.
- Normal GET endpoints must hide soft-deleted tickets.
- Deleted listing shows only soft-deleted tickets for the given project.
- Restore sets deleted=false and deletedAt=null.
- Accessing a deleted ticket through normal GET should behave as not found.

Update behavior:
- PATCH /tickets/{ticketId} may update title, description, status, priority, assigneeId, and dueDate.
- If a field is null in UpdateTicketRequest, leave the existing value unchanged.
- For assigneeId: if assigneeId is null, leave assignee unchanged. Do not add complex explicit-null tracking in this prompt.
- Manual priority update should clear isOverdue if priority actually changes, because the PDF says manual priority change resets auto-escalation state. Do not implement the escalation scheduler yet.

Optimistic locking:
- Ticket already has @Version.
- Map ObjectOptimisticLockingFailureException / OptimisticLockException to 409 Conflict if needed.
- Do not expose stack traces.

Audit logging:
- Extend AuditAction enum only with:
    - CREATE_TICKET
    - UPDATE_TICKET
    - DELETE_TICKET
    - RESTORE_TICKET
- Extend AuditLogService minimally with recordTicketAction(...) or a simple generic recordAction(...).
- Record those four ticket actions.
- Do not implement audit-log query endpoint.

Implementation expectations:
- Create TicketService.
- Create TicketController.
- Use existing TicketMapper and DTOs where possible.
- Controllers must return DTOs, not entities.
- Use constructor injection.
- Keep business rules inside TicketService.
- Keep code simple and readable.
- Make the code compile and tests pass.

Endpoint behavior:
- GET /tickets?projectId={projectId} returns List<TicketResponse>.
- GET /tickets/{ticketId} returns TicketResponse.
- POST /tickets returns TicketResponse with 200 OK.
- PATCH /tickets/{ticketId} returns 200 OK with no response body, matching README style.
- DELETE /tickets/{ticketId} returns 200 OK with no response body.
- GET /tickets/deleted?projectId={projectId} returns List<TicketResponse> and requires ADMIN.
- POST /tickets/{ticketId}/restore returns 200 OK. Returning TicketResponse is acceptable if consistent with existing project restore style, but keep it simple.

Validation/errors:
- Missing ticket returns ResourceNotFoundException.
- Missing project returns ResourceNotFoundException.
- Missing assignee user returns ResourceNotFoundException.
- Deleted project should not accept new tickets.
- Updating a DONE ticket should return BadRequestException or ConflictException with a clear message.
- Backward status transition should return BadRequestException or ConflictException with a clear message.
- Non-ADMIN access to deleted/restore endpoints should return 403 Forbidden.
- Invalid request bodies should use the existing validation error response.

Tests:
Add focused tests only for ticket core:
- create ticket with valid project succeeds
- create ticket with missing project fails
- create ticket with missing assignee fails
- get tickets by project hides deleted tickets
- delete ticket soft-deletes it
- normal GET after delete returns 404
- deleted endpoint requires ADMIN
- restore endpoint requires ADMIN
- cannot update ticket once DONE
- cannot move status backward
- ticket state-changing actions write audit log

After changes, tell me:
1. Which files were created or changed.
2. Confirm that you did not implement comments, dependencies, attachments, CSV, workload assignment, escalation, or audit-log querying.
3. Which ticket actions are audited.
4. How lifecycle validation is implemented.
5. How ADMIN-only deleted/restore access is enforced.
6. The exact command I should run to verify.








Prompt 7 — Comments and mentions:

Implement ONLY Comments API and @mention mechanism according to README.md and the assignment requirements.

Hard scope limit:
Do NOT implement ticket dependencies.
Do NOT implement attachments.
Do NOT implement CSV import/export.
Do NOT implement workload auto-assignment.
Do NOT implement scheduled escalation.
Do NOT implement audit-log querying.
Do NOT implement new ticket core behavior except what is strictly needed to validate that a ticket exists and is not deleted.
If you need any of those future features, stop and explain why instead of implementing them.

Before editing, inspect:
- README.md
- Comment entity
- Mention entity
- CommentRepository
- MentionRepository
- TicketService / TicketRepository
- UserService / UserRepository
- AuditLogService and AuditAction enum
- existing Comment DTOs and CommentMapper
- existing exceptions and GlobalExceptionHandler
- existing SecurityConfig / UserPrincipal

Contract notes:
- README.md defines:
    - GET /tickets/:ticketId/comments
    - POST /tickets/:ticketId/comments
    - PATCH /tickets/:ticketId/comments/:commentId
    - DELETE /tickets/:ticketId/comments/:commentId
    - GET /users/:userId/mentions?page=&pageSize=
- The PDF requires:
    - users can leave comments on tickets
    - comments include content and authorId
    - two users cannot edit a comment at the same time
    - @username mentions are matched case-insensitively
    - mention associations are persisted
    - comment responses include mentionedUsers
    - on comment update, mention list is re-evaluated: newly added mentions are created and removed mentions are deleted
    - user mentions are retrievable newest first

Endpoints to implement:
- GET /tickets/{ticketId}/comments
- POST /tickets/{ticketId}/comments
- PATCH /tickets/{ticketId}/comments/{commentId}
- DELETE /tickets/{ticketId}/comments/{commentId}
- GET /users/{userId}/mentions?page=&pageSize=

Security:
- All comment and mention endpoints must remain protected by JWT.
- Do not weaken existing JWT/security behavior.
- No role restriction is required for basic comment/mention endpoints unless README/PDF explicitly says so.

Comment rules:
- ticketId must reference an existing non-deleted ticket.
- authorId must reference an existing user.
- Add comment with authorId and content.
- Fetch comments for a ticket sorted by creation order unless README says otherwise.
- Update comment content only.
- Delete comment by ticketId + commentId.
- Comment has @Version already; preserve optimistic locking.
- Map ObjectOptimisticLockingFailureException / OptimisticLockException to 409 Conflict if needed.
- Do not expose entities directly.

Mention rules:
- Parse @username from comment content.
- Mention matching must be case-insensitive.
- Persist one mention association per mentioned user per comment.
- Do not create duplicate mentions for the same comment/user.
- If @username does not match any user, ignore it rather than failing the entire comment.
- On comment update:
    - delete old mentions for that comment
    - re-parse the updated content
    - persist the new mention set
- Include mentionedUsers in every CommentResponse.
- GET /users/{userId}/mentions returns mentioned comments newest first.
- Support optional page and pageSize query params.
- Use sensible defaults, for example page=0 and pageSize=20, unless README implies otherwise.

Audit logging:
- Extend AuditAction enum only with:
    - ADD_COMMENT
    - UPDATE_COMMENT
    - DELETE_COMMENT
- Record those three comment actions using existing AuditLogService.
- Do not implement audit-log query endpoint in this prompt.

Implementation expectations:
- Create CommentService.
- Create CommentController.
- Use existing CommentMapper and DTOs where possible.
- Extend UserController or add a small MentionController only for GET /users/{userId}/mentions if that is cleaner.
- Controllers must return DTOs, not entities.
- Use constructor injection.
- Keep business rules inside CommentService.
- Keep mention parsing simple and readable, using a small helper method or private method.
- Make the code compile and tests pass.

Endpoint behavior:
- GET /tickets/{ticketId}/comments returns List<CommentResponse>.
- POST /tickets/{ticketId}/comments returns CommentResponse with 200 OK.
- PATCH /tickets/{ticketId}/comments/{commentId} returns 200 OK with no response body, matching README style.
- DELETE /tickets/{ticketId}/comments/{commentId} returns 200 OK with no response body.
- GET /users/{userId}/mentions?page=&pageSize= returns MentionsPageResponse.

Validation/errors:
- Missing ticket returns ResourceNotFoundException.
- Deleted ticket should behave as not found.
- Missing author user returns ResourceNotFoundException.
- Missing mentioned user in GET /users/{userId}/mentions returns ResourceNotFoundException.
- Missing comment returns ResourceNotFoundException.
- Invalid request bodies should use existing validation error response.
- Concurrent edit conflicts should return 409 Conflict if detected by JPA optimistic locking.

Tests:
Add focused tests only for comments and mentions:
- add comment to existing ticket succeeds
- add comment with missing ticket fails
- add comment with missing author fails
- get comments for ticket includes mentionedUsers
- mention parsing is case-insensitive
- duplicate @username in same comment creates only one mention association
- updating a comment re-evaluates mentions and removes old mentions
- deleting a comment removes or cascades mention associations cleanly
- GET /users/{userId}/mentions returns newest first and paginated response
- comment state-changing actions write audit log

After changes, tell me:
1. Which files were created or changed.
2. Confirm that you did not implement dependencies, attachments, CSV, workload assignment, escalation, or audit-log querying.
3. Which comment actions are audited.
4. How mention parsing and case-insensitive matching are implemented.
5. How mention re-evaluation on update is implemented.
6. The exact command I should run to verify.









Prompt 8 — Dependencies:
Implement ONLY Ticket Dependencies API according to README.md and the assignment requirements.

Hard scope limit:
Do NOT implement attachments.
Do NOT implement CSV import/export.
Do NOT implement workload auto-assignment.
Do NOT implement scheduled escalation.
Do NOT implement audit-log querying.
Do NOT implement new comments/mentions behavior.
If you need any of those future features, stop and explain why instead of implementing them.

Before editing, inspect:
- README.md
- Ticket entity
- TicketRepository
- TicketDependency entity
- TicketDependencyRepository
- TicketService
- TicketController
- AuditLogService and AuditAction enum
- existing DTOs and mappers, especially AddDependencyRequest and DependencyResponse
- existing exceptions and GlobalExceptionHandler
- existing SecurityConfig / UserPrincipal

Contract notes:
- README.md / PDF require ticket dependencies:
    - POST /tickets/{ticketId}/dependencies with body { "blockedBy": 42 }
    - GET /tickets/{ticketId}/dependencies
    - DELETE /tickets/{ticketId}/dependencies/{blockerId}
- Meaning: ticketId is blocked by blockerId.
- Both tickets must exist.
- Both tickets must belong to the same project.
- A ticket cannot transition to DONE if it has unresolved blockers.
- Unresolved blocker means blocker.status != DONE.

Endpoints to implement:
- POST /tickets/{ticketId}/dependencies
- GET /tickets/{ticketId}/dependencies
- DELETE /tickets/{ticketId}/dependencies/{blockerId}

Security:
- All dependency endpoints must remain protected by JWT.
- No ADMIN-only restriction is required unless README/PDF explicitly says so.
- Do not weaken existing JWT/security behavior.

Dependency rules:
- Both ticketId and blockedBy/blockerId must reference existing non-deleted tickets.
- Both tickets must belong to the same project.
- A ticket cannot depend on itself.
- Duplicate dependency should return ConflictException / 409 Conflict.
- GET /tickets/{ticketId}/dependencies returns all tickets that this ticket is blocked by.
- DELETE removes the dependency between ticketId and blockerId.
- Deleting a non-existing dependency should return ResourceNotFoundException or ConflictException with a clear message.
- Keep the implementation simple and readable.

DONE transition integration:
- Update TicketService lifecycle validation so that a ticket cannot transition to DONE if it has unresolved blockers.
- This applies when PATCH /tickets/{ticketId} attempts to set status = DONE.
- Unresolved blocker means any blocker ticket for that ticket has status != DONE.
- If unresolved blockers exist, return ConflictException / 409 Conflict with a clear message.
- Do not change any other ticket lifecycle rules.

Audit logging:
- Use existing AuditAction values if already present:
    - ADD_DEPENDENCY
    - REMOVE_DEPENDENCY
- If they do not exist, add only those two values.
- Extend AuditLogService minimally if needed with recordDependencyAction(...) or reuse a generic recordAction(...).
- Record add/remove dependency actions.
- Do not implement audit-log query endpoint.

Implementation expectations:
- Create TicketDependencyService, or add a small dedicated dependency section to TicketService only if that is clearly cleaner.
- Prefer a separate TicketDependencyService for clarity.
- Add dependency endpoints to TicketController or create TicketDependencyController if cleaner.
- Use existing AddDependencyRequest and DependencyResponse.
- Controllers must return DTOs, not entities.
- Use constructor injection.
- Keep business rules inside the service layer.
- Make the code compile and tests pass.

Endpoint behavior:
- POST /tickets/{ticketId}/dependencies returns 200 OK. Returning DependencyResponse is acceptable if useful.
- GET /tickets/{ticketId}/dependencies returns List<DependencyResponse>.
- DELETE /tickets/{ticketId}/dependencies/{blockerId} returns 200 OK with no response body.

Validation/errors:
- Missing ticket returns ResourceNotFoundException.
- Deleted ticket behaves as not found.
- Missing blocker returns ResourceNotFoundException.
- Self-dependency returns BadRequestException or ConflictException.
- Cross-project dependency returns BadRequestException or ConflictException.
- Duplicate dependency returns ConflictException.
- Unresolved blockers when moving to DONE returns ConflictException.
- Invalid request bodies use existing validation error response.

Tests:
Add focused tests only for dependencies:
- add dependency between tickets in same project succeeds
- list dependencies returns blocker
- remove dependency succeeds
- self-dependency fails
- duplicate dependency fails
- cross-project dependency fails
- missing blocker fails
- ticket cannot transition to DONE while blocker is unresolved
- ticket can transition to DONE after blocker is DONE
- add/remove dependency actions write audit log

After changes, tell me:
1. Which files were created or changed.
2. Confirm that you did not implement attachments, CSV, workload assignment, escalation, or audit-log querying.
3. Which dependency actions are audited.
4. How unresolved blockers are checked during DONE transition.
5. The exact command I should run to verify.






Prompt 9 — Auto-assignment and workload:

Implement ONLY auto-assignment and project workload according to README.md and the assignment requirements.

Hard scope limit:
Do NOT implement attachments.
Do NOT implement CSV import/export.
Do NOT implement scheduled escalation.
Do NOT implement audit-log querying.
Do NOT implement new comments/mentions behavior.
Do NOT implement new dependency endpoints.
Do NOT add ProjectMember or any new project membership model.
If you need any of those future features, stop and explain why instead of implementing them.

Before editing, inspect:
- README.md
- TicketService
- TicketRepository
- ProjectService / ProjectRepository
- User entity / UserRepository / UserService
- Role enum
- AuditLogService and AuditAction enum
- WorkloadResponse DTO
- existing exceptions and GlobalExceptionHandler
- existing SecurityConfig / UserPrincipal

Contract notes:
- The PDF requires auto-assignment for tickets without an explicit assignee.
- The PDF says the system should assign to the least-loaded DEVELOPER in the project.
- There is no ProjectMember API/table in the README.
- Therefore, use this documented interpretation:
    - Candidate users are all users with role DEVELOPER.
    - Workload is counted per project.
    - ADMIN users are excluded.
    - This assumption will be documented later in run.md.
- Do not add ProjectMember unless it already exists, and it does not currently exist.

Endpoint to implement:
- GET /projects/{projectId}/workload

Security:
- The workload endpoint must remain protected by JWT.
- No ADMIN-only restriction is required unless README/PDF explicitly says so.
- Do not weaken existing JWT/security behavior.

Auto-assignment rules:
- Auto-assignment happens only during ticket creation.
- If CreateTicketRequest.assigneeId is present/non-null, use the explicit assignee exactly as currently implemented.
- If CreateTicketRequest.assigneeId is absent/null, automatically assign the ticket to the least-loaded DEVELOPER.
- Workload is the count of non-DONE, non-deleted tickets assigned to that user in the same project.
- ADMIN users are excluded from auto-assignment candidates.
- Ties are broken by user registration order, oldest first. Use user id ascending as the practical tie-breaker unless createdAt is easier and reliable.
- If no DEVELOPER users exist, create the ticket with assignee = null.
- Auto-assignment must not run on ticket update.
- PATCH /tickets/{id} assigneeId should keep its current explicit behavior and must not trigger auto-assignment.

Workload endpoint rules:
- GET /projects/{projectId}/workload returns workload information for DEVELOPER users.
- projectId must reference an existing non-deleted project.
- Workload count should include only tickets in that project where:
    - assignee is that developer
    - ticket is not deleted
    - ticket status is not DONE
- Exclude ADMIN users from workload response.
- Use existing WorkloadResponse DTO if possible.
- Keep the response simple and aligned with existing DTO style.

Audit logging:
- Add/use AuditAction.AUTO_ASSIGN.
- When auto-assignment assigns a ticket to a developer, record an audit log entry.
- The audit entry should have actor = SYSTEM if possible.
- If AuditLogService currently assumes USER, extend it minimally to support a system action.
- Do not implement audit-log query endpoint.

Implementation expectations:
- Prefer adding a small helper method inside TicketService or a small AutoAssignmentService only if cleaner.
- Keep business logic readable and not duplicated.
- Use existing repositories where possible.
- Add repository methods only if needed.
- Use constructor injection.
- Make the code compile and tests pass.

Tests:
Add focused tests only for auto-assignment and workload:
- creating a ticket with explicit assignee keeps that assignee
- creating a ticket without assignee auto-assigns least-loaded DEVELOPER
- ADMIN users are excluded from auto-assignment
- ties are broken by oldest registration / lowest id
- if no DEVELOPER exists, ticket is created with null assignee
- auto-assignment is not triggered on PATCH
- workload endpoint returns non-DONE non-deleted ticket counts per developer in the project
- DONE tickets are excluded from workload count
- deleted tickets are excluded from workload count
- auto-assignment records AUTO_ASSIGN audit log with SYSTEM actor

After changes, tell me:
1. Which files were created or changed.
2. Confirm that you did not implement attachments, CSV, escalation, or audit-log querying.
3. How auto-assignment chooses a developer.
4. How workload is calculated.
5. How AUTO_ASSIGN audit logging is implemented.
6. The exact command I should run to verify.






Prompt 10 — CSV import/export:
Implement ONLY ticket CSV import/export according to README.md and the assignment requirements PDF.

Hard scope limit:
Do NOT implement attachments.
Do NOT implement scheduled escalation.
Do NOT implement audit-log querying.
Do NOT implement new comments/mentions behavior.
Do NOT implement new dependency behavior.
Do NOT change auto-assignment/workload behavior except by reusing normal ticket creation rules during import.
If you need any future feature, stop and explain why instead of implementing it.

Before editing, inspect:
- README.md
- Instructions.md
- pom.xml
- TicketService
- TicketRepository
- ProjectService / ProjectRepository
- UserService / UserRepository
- AuditLogService and AuditAction enum
- ImportTicketsResponse DTO
- CreateTicketRequest / TicketResponse / TicketMapper
- existing exceptions and GlobalExceptionHandler
- existing SecurityConfig / UserPrincipal

Contract notes:
- README.md defines:
    - GET /tickets/export?projectId={projectId}
    - POST /tickets/import with multipart/form-data:
        - file = CSV file
        - projectId = target project id
- Export response is CSV with fields:
    - id, title, description, status, priority, type, assigneeId
- Import response is:
    - { "created": number, "failed": number, "errors": [...] }
- Apache Commons CSV already exists in pom.xml:
    - org.apache.commons:commons-csv:1.10.0
- Use Apache Commons CSV.
- CSV must correctly handle commas, quotes, and newlines.

Endpoints to implement:
- GET /tickets/export?projectId={projectId}
- POST /tickets/import

Security:
- Both import/export endpoints must remain protected by JWT.
- No ADMIN-only restriction is required unless README/PDF explicitly says so.
- Do not weaken existing JWT/security behavior.

Export rules:
- projectId must reference an existing non-deleted project.
- Export only non-deleted tickets for the project.
- Return text/csv.
- Add Content-Disposition attachment header with a useful filename, for example:
  tickets-project-{projectId}.csv
- Include exactly these columns in this order:
  id,title,description,status,priority,type,assigneeId
- Use Apache Commons CSV so commas, quotes, and newlines are escaped correctly.
- For null description or assigneeId, write an empty CSV value.
- Do not include dueDate in export because README/PDF CSV fields do not include it.

Import rules:
- Target projectId comes from the multipart form field, not from the CSV.
- projectId must reference an existing non-deleted project.
- Parse CSV safely using Apache Commons CSV, including commas, quotes, and newlines.
- Do not abort the whole import because of one bad row.
- Successful rows should still create tickets even if other rows fail.
- Return ImportTicketsResponse with:
    - created
    - failed
    - errors
- Include useful row-level error messages with row numbers.

Import CSV format:
- Expected columns:
  id,title,description,status,priority,type,assigneeId
- id from CSV should be ignored because new database IDs are generated.
- title is required.
- description is optional.
- status is required and must be one of:
  TODO, IN_PROGRESS, IN_REVIEW, DONE
- priority is required and must be one of:
  LOW, MEDIUM, HIGH, CRITICAL
- type is required and must be one of:
  BUG, FEATURE, TECHNICAL
- assigneeId is optional.
- If assigneeId is present and non-empty, it must reference an existing user.
- If assigneeId is missing or empty, reuse normal TicketService.createTicket behavior, including auto-assignment.
- dueDate should remain null on imported tickets because the CSV contract does not include dueDate.

Validation/creation behavior:
- Reuse TicketService.createTicket for valid rows so normal validation, active-project checks, explicit assignee lookup, auto-assignment, CREATE_TICKET audit logs, and AUTO_ASSIGN audit logs remain consistent.
- Since @Valid validation is controller-driven, the CSV service must validate parsed row values itself before constructing CreateTicketRequest and calling TicketService.createTicket.
- A bad enum value, missing required field, invalid assigneeId, or malformed row should increase failed count and add an error message, not crash the whole import.

Audit logging:
- Use existing AuditAction.IMPORT.
- Add a minimal AuditLogService method if needed, for example recordImportAction(projectId).
- Record one IMPORT audit action after import completes.
- Prefer entityType = PROJECT and entityId = projectId because import is project-level.
- Actor should be the authenticated user if available.
- Do not implement audit-log query endpoint.

Implementation expectations:
- Create a small TicketCsvService or TicketImportExportService.
- Add endpoints to TicketController or create a dedicated TicketCsvController if cleaner.
- Prefer a dedicated service for parsing/exporting logic.
- Use constructor injection.
- Keep code simple and readable.
- Make the code compile and tests pass.
- Do not add new dependencies because Commons CSV already exists.

Endpoint behavior:
- GET /tickets/export?projectId={projectId} returns 200 OK with CSV content.
- POST /tickets/import returns 200 OK with ImportTicketsResponse.
- Missing/empty file should return BadRequestException or validation error.
- Invalid project should return ResourceNotFoundException.
- Bad rows should be counted in failed and reported in errors, not crash the entire request.

Tests:
Add focused tests only for CSV import/export:
- export returns text/csv
- export includes the required header columns in exact order
- export includes only non-deleted tickets for the project
- export correctly handles commas, quotes, and newlines in title/description
- import valid CSV creates tickets
- import ignores CSV id and generates new database IDs
- import with invalid row continues processing later valid rows
- import reports failed count and row-level errors
- import validates bad enum values
- import validates missing title
- import validates missing assignee
- import with missing/empty assigneeId triggers normal auto-assignment
- import action writes audit log

After changes, tell me:
1. Which files were created or changed.
2. Confirm that you did not implement attachments, escalation, audit-log querying, new comments/mentions behavior, or new dependency behavior.
3. Which CSV library is used.
4. How export handles quotes, commas, and newlines.
5. How import handles bad rows without aborting.
6. How import reuses normal ticket creation and auto-assignment rules.
7. How IMPORT audit logging is implemented.
8. The exact command I should run to verify.









Prompt 11 — Attachments:
Implement ONLY attachment upload/delete according to README.md and the assignment requirements .

Hard scope limit:
Do NOT implement scheduled escalation.
Do NOT implement audit-log querying.
Do NOT implement new comments/mentions behavior.
Do NOT implement new dependency behavior.
Do NOT change CSV import/export behavior.
Do NOT add external storage services.
If you need any future feature, stop and explain why instead of implementing it.

Before editing, inspect:
- README.md
- Instructions.md
- Attachment entity
- AttachmentRepository
- AttachmentResponse DTO
- AttachmentMapper
- TicketService / TicketRepository
- AuditLogService and AuditAction enum
- existing exceptions and GlobalExceptionHandler
- existing SecurityConfig / UserPrincipal

Contract notes:
- README/PDF require attachment upload and delete.
- Endpoints:
    - POST /tickets/{ticketId}/attachments with multipart/form-data field file
    - DELETE /tickets/{ticketId}/attachments/{attachmentId}
- Max file size: 10 MB.
- Allowed content types:
    - image/png
    - image/jpeg
    - application/pdf
    - text/plain
- Reject all other content types with a clear error.
- The current Attachment entity already stores:
    - ticket
    - filename
    - contentType
    - sizeBytes
    - content byte[]
- Therefore, store attachment content in PostgreSQL using the existing Attachment.content field.
- Do NOT switch to local disk storage in this prompt.
- No uploads/ directory is needed if content is stored in DB.

Endpoints to implement:
- POST /tickets/{ticketId}/attachments
- DELETE /tickets/{ticketId}/attachments/{attachmentId}

Optional endpoint:
- If simple and already supported by existing repository/mapper, also implement:
    - GET /tickets/{ticketId}/attachments
- Do NOT implement download unless it is extremely simple and does not distract from required upload/delete behavior.

Security:
- Attachment endpoints must remain protected by JWT.
- No ADMIN-only restriction is required unless README/PDF explicitly says so.
- Do not weaken existing JWT/security behavior.

Upload rules:
- ticketId must reference an existing non-deleted ticket.
- Multipart field name must be file.
- File must not be missing or empty.
- File size must be <= 10 MB.
- Content type must be one of the allowed values.
- Filename should be sanitized to a simple safe filename using the original filename where possible.
- Store:
    - ticket
    - filename
    - contentType
    - sizeBytes
    - content bytes
- Return AttachmentResponse with:
    - id
    - ticketId
    - filename
    - contentType
    - sizeBytes
- Do not return file content in normal JSON responses.

Delete rules:
- ticketId must reference an existing non-deleted ticket.
- attachmentId must reference an attachment belonging to that ticket.
- Delete attachment row from PostgreSQL.
- Return 200 OK with no response body.
- Missing attachment should return ResourceNotFoundException.

Validation/errors:
- Missing/deleted ticket returns ResourceNotFoundException.
- Missing file returns BadRequestException.
- Empty file returns BadRequestException.
- File larger than 10 MB returns BadRequestException.
- Unsupported content type returns BadRequestException with clear allowed-type message.
- Invalid multipart request should use existing error handling if possible.

Audit logging:
- Use existing AuditAction values if present:
    - UPLOAD_ATTACHMENT
    - DELETE_ATTACHMENT
- If missing, add only those values.
- Extend AuditLogService minimally with recordAttachmentAction(...), or reuse a generic helper.
- Record upload/delete attachment actions.
- Prefer entityType = ATTACHMENT if it already exists.
- If AuditEntityType.ATTACHMENT does not exist, add only that enum value.
- Do not implement audit-log query endpoint.

Implementation expectations:
- Create AttachmentService.
- Create AttachmentController.
- Use existing AttachmentMapper and AttachmentResponse where possible.
- Use constructor injection.
- Keep business logic inside AttachmentService.
- Keep implementation minimal and reliable.
- Make the code compile and tests pass.

Tests:
Add focused tests only for attachments:
- upload valid text/plain file succeeds
- upload valid image/png or application/pdf succeeds if easy to mock
- upload missing/empty file fails
- upload unsupported content type fails
- upload file larger than 10 MB fails
- upload to missing ticket fails
- delete attachment succeeds
- delete missing attachment fails
- attachment upload/delete actions write audit log
- attachment endpoints require JWT

After changes, tell me:
1. Which files were created or changed.
2. Confirm that you did not implement escalation, audit-log querying, or unrelated behavior.
3. Whether attachment content is stored in PostgreSQL or local disk.
4. Which content types are allowed.
5. How the 10 MB limit is enforced.
6. Which attachment actions are audited.
7. The exact command I should run to verify.





Prompt 12 — Auto escalation scheduler:
Implement ONLY automatic overdue ticket escalation according to README.md and the assignment requirements PDF.

Hard scope limit:
Do NOT implement audit-log querying.
Do NOT implement new attachment behavior.
Do NOT implement new CSV behavior.
Do NOT implement new comments/mentions behavior.
Do NOT implement new dependency behavior.
Do NOT change ticket lifecycle rules except for priority/isOverdue escalation behavior.
If you need any future feature, stop and explain why instead of implementing it.

Before editing, inspect:
- README.md
- Instructions.md
- Ticket entity
- TicketRepository
- TicketService
- TicketPriority enum
- TicketStatus enum
- AuditLogService and AuditAction enum
- existing tests in IssueFlowApplicationTests
- existing application configuration
- existing SecurityConfig / main application class

Contract notes:
- Ticket create/update already accepts dueDate as ISO-8601 datetime.
- Ticket already has dueDate and isOverdue fields.
- Escalation applies only to tickets with dueDate set and dueDate < now.
- Ignore DONE tickets.
- Ignore deleted tickets.
- For overdue tickets below CRITICAL, promote priority by one level:
  LOW -> MEDIUM
  MEDIUM -> HIGH
  HIGH -> CRITICAL
- If a ticket is CRITICAL and still overdue, set isOverdue = true.
- Escalation must not change ticket status.
- Escalation must be idempotent:
    - CRITICAL is never escalated beyond CRITICAL.
    - Re-running escalation should not keep changing a CRITICAL ticket beyond setting isOverdue=true.
- Manual priority change through PATCH /tickets/{id} already clears isOverdue and should remain that way.
- Automatic escalation should record audit logs with actor = SYSTEM and action = AUTO_ESCALATE.

Implementation:
- Create a small TicketEscalationService, or similar.
- Expose a public service method that tests can call directly, for example escalateOverdueTickets().
- Add a scheduled method using Spring @Scheduled that calls the service method.
- Enable scheduling if it is not already enabled.
- Use a simple fixed delay or cron expression. Keep it reasonable and easy to understand.
- Do not add external scheduling infrastructure.
- Use Instant.now() or inject Clock if simple. Prefer Clock if it makes tests cleaner without overengineering.
- Use TicketRepository.findByDueDateBeforeAndStatusNotAndDeletedFalse(...) if suitable.
- Save changed tickets through the repository or rely on transaction dirty checking.
- Record AUTO_ESCALATE system audit action only when an actual change occurs:
    - priority changed, or
    - isOverdue changed from false to true.
- Do not record duplicate audit logs for unchanged tickets on repeated runs.

Priority escalation rules:
- LOW becomes MEDIUM.
- MEDIUM becomes HIGH.
- HIGH becomes CRITICAL.
- CRITICAL stays CRITICAL and isOverdue becomes true.
- DONE tickets are ignored.
- Tickets with null dueDate are ignored.
- Future dueDate tickets are ignored.
- Deleted tickets are ignored.

Tests:
Add focused tests only for escalation:
- overdue LOW ticket becomes MEDIUM.
- overdue MEDIUM ticket becomes HIGH.
- overdue HIGH ticket becomes CRITICAL.
- overdue CRITICAL ticket sets isOverdue=true.
- DONE ticket is ignored.
- future dueDate ticket is ignored.
- ticket without dueDate is ignored.
- deleted ticket is ignored if easy with existing helpers.
- escalation does not change status.
- repeated escalation is idempotent for CRITICAL overdue ticket.
- AUTO_ESCALATE audit log is written only when a change occurs.
- manual PATCH priority change still clears isOverdue.

After changes, tell me:
1. Which files were created or changed.
2. Confirm that you did not implement audit-log querying, attachment changes, CSV changes, comments/mentions changes, or dependency changes.
3. How the scheduler is configured.
4. How the public testable escalation method works.
5. How priority escalation and isOverdue behavior are implemented.
6. How AUTO_ESCALATE audit logging avoids duplicate logs when nothing changes.
7. The exact command I should run to verify.










Prompt 13 — Audit Log API:
Implement ONLY the read-only Audit Log API according to README.md and the assignment requirements PDF.

Hard scope limit:
Do NOT implement new write-side audit behavior unless strictly needed for mapping/querying.
Do NOT change existing audit logging calls for projects, tickets, comments, dependencies, attachments, import, auto-assignment, or auto-escalation.
Do NOT implement new ticket/project/comment/dependency/attachment/CSV/escalation behavior.
Do NOT add pagination unless README/PDF explicitly requires it.
If you need any unrelated feature, stop and explain why instead of implementing it.

Before editing, inspect:
- README.md
- Instructions.md
- AuditLog entity
- AuditLogRepository
- AuditLogResponse DTO
- AuditLogMapper
- AuditLogService
- AuditAction enum
- AuditEntityType enum
- ActorType enum
- existing exceptions and GlobalExceptionHandler
- existing SecurityConfig / UserPrincipal

Contract notes:
- README.md defines:
    - GET /audit-logs
- Optional query params:
    - entityType
    - entityId
    - action
    - actor
- Response status:
    - 200 OK
- Response body example:
  [
  {
  "id": 1,
  "action": "CREATE",
  "entityType": "TICKET",
  "entityId": 5,
  "performedBy": 2,
  "actor": "USER",
  "timestamp": "2026-03-01T10:00:00Z"
  }
  ]
- This API is read-only.
- Existing code already records audit logs for state-changing actions.
- This prompt should expose/query those records only.

Endpoint to implement:
- GET /audit-logs

Security:
- Endpoint must remain protected by JWT.
- No public access.
- Unless README/PDF explicitly requires ADMIN-only, keep it authenticated-only.
- Do not weaken existing JWT/security behavior.

Filtering rules:
- All query params are optional.
- If no query params are provided, return all audit logs.
- Support filtering by any combination of:
    - entityType
    - entityId
    - action
    - actor
- entityType should map to AuditEntityType enum.
- action should map to AuditAction enum.
- actor should map to ActorType enum.
- Invalid enum query params should return a clear 400 Bad Request.
- Sort results newest first by timestamp descending.

Implementation expectations:
- Create AuditLogController.
- Add a query method to AuditLogService, or a small dedicated method in the existing service.
- Use existing AuditLogResponse DTO and AuditLogMapper.
- Use DTOs, not entities, in controller responses.
- Keep implementation simple and readable.
- Prefer service-layer filtering.
- If repository support is needed, add simple query methods or use JpaSpecificationExecutor only if not overcomplicated.
- Make the code compile and tests pass.

Repository/query guidance:
- Since all filters are optional, avoid writing many duplicated repository methods if possible.
- A simple Criteria/Specification approach is acceptable if clean.
- A simple in-service filtering approach is acceptable only if the dataset is small and the code stays readable, but repository-level filtering is preferable.
- Do not over-engineer.

Tests:
Add focused tests only for Audit Log API:
- GET /audit-logs requires authentication.
- GET /audit-logs returns audit logs newest first.
- filter by entityType works.
- filter by entityId works.
- filter by action works.
- filter by actor works.
- combined filters work.
- invalid entityType returns 400.
- invalid action returns 400.
- invalid actor returns 400.
- response uses AuditLogResponse and does not expose entity internals.

After changes, tell me:
1. Which files were created or changed.
2. Confirm that you did not implement unrelated business features.
3. Which filters are supported.
4. How invalid enum query params are handled.
5. How newest-first sorting is enforced.
6. The exact command I should run to verify.








Prompt 14 — Final tests / regression tests

Implement ONLY final regression-test cleanup for IssueFlow.

Hard scope limit:
Do NOT implement new production features.
Do NOT change API behavior unless a test reveals a clear bug against README.md .
Do NOT refactor the whole application.
Do NOT rewrite the entire test class.
Do NOT chase 100% coverage.
Do NOT add random or redundant tests just to increase the test count.

Before editing, inspect:
- README.md
- TDP_issueflow_requirements.pdf
- IssueFlowApplicationTests.java
- existing controllers/services/repositories only as needed to understand behavior

Current state:
The project already has integration tests using SpringBootTest and MockMvc.
There are already tests for:
- users/auth basics
- projects
- tickets
- comments/mentions
- dependencies
- auto-assignment/workload
- CSV import/export
- attachments
- auto-escalation
- audit-log API

Task:
Review the existing tests and add only meaningful missing regression tests that prove important README/PDF behavior.

Focus on these possible gaps:
1. User creation rejects invalid role with 400.
2. GET /users returns users without passwordHash.
3. GET /users/{userId} returns the expected user DTO.
4. POST /users/update/{userId} persists fullName/role changes.
5. DELETE /users/{userId} works if implemented by the current API.
6. GET /auth/me returns the current authenticated user.
7. PATCH /projects/{projectId} persists name/description updates.
8. GET /projects/deleted returns soft-deleted projects for ADMIN.
9. POST /projects/{projectId}/restore actually restores project visibility.
10. POST /tickets/{ticketId}/restore actually restores ticket visibility.
11. Invalid ticket enum values return clear 400 Bad Request.
12. Missing required ticket title returns validation error.
13. CSV import with missing/empty file returns 400.
14. Any other obvious high-value README/PDF behavior that is currently not tested.

Rules:
- Keep tests deterministic.
- Prefer MockMvc integration tests consistent with the existing style.
- Reuse existing helper methods where possible.
- Avoid duplicate tests that already prove the same behavior.
- If an existing test is redundant but harmless, leave it alone.
- Only remove a test if it is clearly wrong or obsolete, and explain why.
- Keep the test class readable.
- Ensure ./mvnw test passes.

After changes, tell me:
1. Which tests were added.
2. Which gaps they cover.
3. Whether any tests were removed or changed, and why.
4. Confirm that no production feature was implemented.
5. The exact command I should run to verify.








