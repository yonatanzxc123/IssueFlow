# IssueFlow - API Testing Log

This document contains a record of the manual API testing conducted via Postman throughout the development of IssueFlow.

The tests are categorized by major implementation stages (e.g., Foundation Entities, User Management, Ticket Core, and Business Rules) to demonstrate that both standard operations and edge cases were successfully verified at each step.



## Table of Contents
| Stage | Checked Topics                       | 
|-------|--------------------------------------| 
| 1     | Users API and JWT authentication     |
| 2     | Project API                          | 
| 3     | Ticket API core                      | 
| 4     | Comments API and @mention mechanism  | 
| 5     | Ticket dependencies API              |
| 6     | Auto-assignment and project workload |
| 7     | CSV import/export                    | 
| 8     | Attachment API                       | 
| 9     | Auto-escalation scheduler            | 
| 10    | Audit Log API                        | 
| 11    | Final tests/ regression tests        |




---

## 1. Stage One - Users API and JWT Authentication

Stage One focused on user management and JWT-based authentication.  
The goal was to verify that users can be created, authenticated, and protected endpoints can only be accessed with a valid non-revoked JWT token.

The Postman tests were executed manually after starting the PostgreSQL container and running the Spring Boot application locally.

---

Test 1 - Create user without password  
(no Bearer token required)

**Method** POST  
**URL** http://localhost:8080/users

**Result** 200 OK  
**Body** UserResponse returned, including id, username, email, fullName, and role.

Purpose: verified that `POST /users` remains compatible with the provided README contract, where the request body does not include a password. The system assigns the default development password `secret`, hashes it with BCrypt, and does not return `passwordHash`.

---

Test 2 - Login with default password  
(no Bearer token required)

**Method** POST  
**URL** http://localhost:8080/auth/login

**Result** 200 OK  
**Body** AuthTokenResponse returned, including accessToken, tokenType, and expiresIn.

Purpose: verified that a user created without an explicit password can log in using the documented default development password `secret`.

---

Test 3 - Access protected endpoint without token

**Method** GET  
**URL** http://localhost:8080/users

**Result** 401 Unauthorized

Purpose: verified that protected endpoints cannot be accessed without a JWT token.

---

Test 4 - Access protected endpoint with valid token  
(using Bearer token from login)

**Method** GET  
**URL** http://localhost:8080/users

**Result** 200 OK  
**Body** List of users returned.

Purpose: verified that protected endpoints can be accessed with a valid JWT token.

---

Test 5 - Get current authenticated user  
(using Bearer token)

**Method** GET  
**URL** http://localhost:8080/auth/me

**Result** 200 OK  
**Body** Current authenticated user profile returned.

Purpose: verified that the server can identify the authenticated user from the JWT token.

---

Test 6 - Update user  
(using Bearer token)

**Method** POST  
**URL** http://localhost:8080/users/update/{userId}

**Result** 200 OK  
**Body** empty

Purpose: verified that an authenticated request can update a user’s full name and role.

---

Test 7 - Verify updated user  
(using Bearer token)

**Method** GET  
**URL** http://localhost:8080/users/{userId}

**Result** 200 OK  
**Body** UserResponse returned with updated fullName and role.

Purpose: verified that the user update was persisted correctly.

---

Test 8 - Logout  
(using Bearer token)

**Method** POST  
**URL** http://localhost:8080/auth/logout

**Result** 200 OK  
**Body** empty

Purpose: verified that logout invalidates the current JWT token by storing it in the revoked-token deny-list.

---

Test 9 - Reuse token after logout  
(using the same old Bearer token)

**Method** GET  
**URL** http://localhost:8080/auth/me

**Result** 401 Unauthorized

Purpose: verified that a revoked JWT token can no longer access protected endpoints after logout.

---

Stage One Result:

**Maven test result:** 8 tests passed, 0 failures, 0 errors.

This stage verified:
- User creation.
- README-compatible user creation without password.
- BCrypt password hashing.
- JWT login.
- JWT-protected endpoints.
- Current-user lookup through `/auth/me`.
- Token logout using a deny-list.
- Rejection of revoked tokens.
- No password hash returned in API responses.




## 2. Stage Two - Projects API

Stage Two focused on the Project API.  
The goal was to verify project creation, project updates, soft delete behavior, restore behavior, JWT protection, ADMIN-only endpoints, and project audit logging.

Before running the project tests, the setup flow was used: create ADMIN user, login as ADMIN, create DEVELOPER user, login as DEVELOPER. The returned user IDs and JWT tokens were stored and reused in the project requests.

---

Test 1 - Create ADMIN user

**Method** POST  
**URL** http://localhost:8080/users

**Result** 200 OK  
**Body** UserResponse returned.

Purpose: created an ADMIN user for testing protected and ADMIN-only project endpoints.

---

Test 2 - Login as ADMIN

**Method** POST  
**URL** http://localhost:8080/auth/login

**Result** 200 OK  
**Body** AuthTokenResponse returned.

Purpose: obtained an ADMIN JWT token for project API requests.

---

Test 3 - Create DEVELOPER user

**Method** POST  
**URL** http://localhost:8080/users

**Result** 200 OK  
**Body** UserResponse returned.

Purpose: created a DEVELOPER user to use as the project owner and to test non-admin access to ADMIN-only endpoints.

---

Test 4 - Login as DEVELOPER

**Method** POST  
**URL** http://localhost:8080/auth/login

**Result** 200 OK  
**Body** AuthTokenResponse returned.

Purpose: obtained a DEVELOPER JWT token for authorization tests.

---

Test 5 - Create project  
(using Bearer adminToken)

**Method** POST  
**URL** http://localhost:8080/projects

**Result** 200 OK  
**Body** ProjectResponse returned, including project id, name, description, and owner information/id.

Purpose: verified that an authenticated user can create a project with a valid ownerId.

---

Test 6 - Get all projects  
(using Bearer adminToken)

**Method** GET  
**URL** http://localhost:8080/projects

**Result** 200 OK  
**Body** List of projects returned. The created project appeared in the list.

Purpose: verified that non-deleted projects are returned by the standard projects list endpoint.

---

Test 7 - Get project by ID  
(using Bearer adminToken or devToken)

**Method** GET  
**URL** http://localhost:8080/projects/{projectId}

**Result** 200 OK  
**Body** ProjectResponse returned.

Purpose: verified that a non-deleted project can be fetched directly by id. This endpoint requires a valid JWT token but does not require ADMIN role.

---

Test 8 - Update project  
(using Bearer adminToken)

**Method** PATCH  
**URL** http://localhost:8080/projects/{projectId}

**Result** 200 OK  
**Body** Updated ProjectResponse returned.

Purpose: verified that project name and description can be updated.

---

Test 9 - Verify project update  
(using Bearer adminToken)

**Method** GET  
**URL** http://localhost:8080/projects/{projectId}

**Result** 200 OK  
**Body** ProjectResponse returned with updated name and description.

Purpose: verified that the project update was persisted correctly.

---

Test 10 - Delete project  
(using Bearer adminToken)

**Method** DELETE  
**URL** http://localhost:8080/projects/{projectId}

**Result** 200 OK  
**Body** empty

Purpose: verified that deleting a project succeeds and performs a soft delete.

---

Test 11 - Normal GET after delete should return 404  
(using Bearer adminToken)

**Method** GET  
**URL** http://localhost:8080/projects/{projectId}

**Result** 404 Not Found

Purpose: verified that soft-deleted projects are hidden from normal project lookup endpoints.

---

Test 12 - Normal project list should hide deleted project  
(using Bearer adminToken)

**Method** GET  
**URL** http://localhost:8080/projects

**Result** 200 OK  
**Body** Deleted project did not appear in the standard project list.

Purpose: verified that soft-deleted projects are hidden from standard project API responses.

---

Test 13 - DEVELOPER tries to list deleted projects  
(using Bearer devToken)

**Method** GET  
**URL** http://localhost:8080/projects/deleted

**Result** 403 Forbidden

Purpose: verified that listing deleted projects is ADMIN-only.

---

Test 14 - ADMIN lists deleted projects  
(using Bearer adminToken)

**Method** GET  
**URL** http://localhost:8080/projects/deleted

**Result** 200 OK  
**Body** Deleted project appeared in the deleted project list.

Purpose: verified that ADMIN users can view soft-deleted projects.

---

Test 15 - DEVELOPER tries to restore deleted project  
(using Bearer devToken)

**Method** POST  
**URL** http://localhost:8080/projects/{projectId}/restore

**Result** 403 Forbidden

Purpose: verified that restoring a deleted project is ADMIN-only.

---

Test 16 - ADMIN restores deleted project  
(using Bearer adminToken)

**Method** POST  
**URL** http://localhost:8080/projects/{projectId}/restore

**Result** 200 OK  
**Body** Restored ProjectResponse returned.

Purpose: verified that ADMIN users can restore soft-deleted projects.

---

Test 17 - Verify restored project is visible again  
(using Bearer adminToken)

**Method** GET  
**URL** http://localhost:8080/projects/{projectId}

**Result** 200 OK  
**Body** Restored ProjectResponse returned.

Purpose: verified that restored projects become visible again through the normal project API.

---

Stage Two Result:

**Maven test result:** 14 tests passed, 0 failures, 0 errors.

This stage verified:
- JWT protection for project endpoints.
- Project creation with valid ownerId.
- Project fetching by id.
- Project listing.
- Project update.
- Project soft delete.
- Deleted project hiding from standard API responses.
- ADMIN-only access for deleted project listing.
- ADMIN-only restore behavior.
- Project restore behavior.
- Project state-changing actions recorded through audit logging.





## 3. Stage Three Ticket Core API

Stage Three focused on the core Ticket API implementation.  
Before running the ticket tests, the same setup flow was used: create ADMIN user, login as ADMIN, create DEVELOPER user, login as DEVELOPER, and create a project. The returned IDs and JWT tokens were stored as Postman collection variables.

The Postman collection was executed successfully and all ticket-core checks passed.

---

Test 1 - Create ticket  
(using Bearer adminToken)

**Method** POST  
**URL** http://localhost:8080/tickets

**Result** 200 OK  
**Body** TicketResponse returned, contains ticket id, project id, assignee id, status, priority, type, and dueDate.

Purpose: verified that a ticket can be created for an existing non-deleted project with a valid optional assignee.

---

Test 2 - Get tickets by project  
(using Bearer adminToken)

**Method** GET  
**URL** http://localhost:8080/tickets?projectId={projectId}

**Result** 200 OK  
**Body** List of tickets for the project. The created ticket appeared in the list.

Purpose: verified that tickets can be fetched by project id.

---

Test 3 - Get ticket by ID  
(using Bearer adminToken)

**Method** GET  
**URL** http://localhost:8080/tickets/{ticketId}

**Result** 200 OK  
**Body** TicketResponse returned for the requested ticket.

Purpose: verified that a non-deleted ticket can be fetched directly by id.

---

Test 4 - Update ticket forward in lifecycle  
(using Bearer adminToken)

**Method** PATCH  
**URL** http://localhost:8080/tickets/{ticketId}

**Result** 200 OK

Purpose: verified that a ticket status can move forward in the lifecycle, for example from TODO to IN_PROGRESS, and that priority can be updated.

---

Test 5 - Verify ticket update  
(using Bearer adminToken)

**Method** GET  
**URL** http://localhost:8080/tickets/{ticketId}

**Result** 200 OK  
**Body** TicketResponse returned with updated status and priority.

Purpose: verified that the PATCH update was persisted correctly.

---

Test 6 - Backward status transition should fail  
(using Bearer adminToken)

**Method** PATCH  
**URL** http://localhost:8080/tickets/{ticketId}

**Result** 409 Conflict

Purpose: verified that backward status transitions are rejected, according to the required lifecycle rule:
TODO → IN_PROGRESS → IN_REVIEW → DONE.

---

Test 7 - Move ticket to DONE  
(using Bearer adminToken)

**Method** PATCH  
**URL** http://localhost:8080/tickets/{ticketId}

**Result** 200 OK

Purpose: verified that a ticket can move forward to DONE.

---

Test 8 - Updating DONE ticket should fail  
(using Bearer adminToken)

**Method** PATCH  
**URL** http://localhost:8080/tickets/{ticketId}

**Result** 409 Conflict

Purpose: verified that once a ticket is DONE, it cannot be updated.

---

Test 9 - Create second ticket for soft delete test  
(using Bearer adminToken)

**Method** POST  
**URL** http://localhost:8080/tickets

**Result** 200 OK  
**Body** TicketResponse returned, contains second ticket id.

Purpose: created a separate non-DONE ticket for testing soft delete and restore behavior.

---

Test 10 - Delete ticket  
(using Bearer adminToken)

**Method** DELETE  
**URL** http://localhost:8080/tickets/{deleteTicketId}

**Result** 200 OK  
**Body** empty

Purpose: verified that deleting a ticket succeeds.

---

Test 11 - Normal GET after delete should return 404  
(using Bearer adminToken)

**Method** GET  
**URL** http://localhost:8080/tickets/{deleteTicketId}

**Result** 404 Not Found

Purpose: verified that soft-deleted tickets are hidden from normal GET endpoints.

---

Test 12 - Normal ticket list should hide deleted ticket  
(using Bearer adminToken)

**Method** GET  
**URL** http://localhost:8080/tickets?projectId={projectId}

**Result** 200 OK  
**Body** Deleted ticket did not appear in the standard ticket list.

Purpose: verified that soft-deleted tickets are hidden from normal project ticket listings.

---

Test 13 - DEVELOPER tries to list deleted tickets  
(using Bearer devToken)

**Method** GET  
**URL** http://localhost:8080/tickets/deleted?projectId={projectId}

**Result** 403 Forbidden

Purpose: verified that deleted-ticket listing is ADMIN-only.

---

Test 14 - ADMIN lists deleted tickets  
(using Bearer adminToken)

**Method** GET  
**URL** http://localhost:8080/tickets/deleted?projectId={projectId}

**Result** 200 OK  
**Body** Deleted ticket appeared in the deleted ticket list.

Purpose: verified that ADMIN users can view soft-deleted tickets.

---

Test 15 - DEVELOPER tries to restore deleted ticket  
(using Bearer devToken)

**Method** POST  
**URL** http://localhost:8080/tickets/{deleteTicketId}/restore

**Result** 403 Forbidden

Purpose: verified that ticket restore is ADMIN-only.

---

Test 16 - ADMIN restores deleted ticket  
(using Bearer adminToken)

**Method** POST  
**URL** http://localhost:8080/tickets/{deleteTicketId}/restore

**Result** 200 OK

Purpose: verified that ADMIN users can restore soft-deleted tickets.

---

Test 17 - Verify restored ticket is visible again  
(using Bearer adminToken)

**Method** GET  
**URL** http://localhost:8080/tickets/{deleteTicketId}

**Result** 200 OK  
**Body** Restored TicketResponse returned.

Purpose: verified that restored tickets become visible again through the normal ticket API.

---

Stage Three Result:

**Postman collection result:** 31 passed checks.  
**Maven test result:** 25 tests passed, 0 failures, 0 errors.

This stage verified:
- JWT protection for ticket endpoints.
- Ticket creation with project and optional assignee.
- Fetching tickets by project and by id.
- Forward-only ticket lifecycle validation.
- Rejection of backward status transitions.
- Rejection of updates to DONE tickets.
- Soft delete behavior.
- Deleted ticket visibility rules.
- ADMIN-only access for deleted ticket listing and restore.
- Ticket restore behavior.


## 4. Stage Four - Comments API and @Mention Mechanism

Stage Four focused on the Comments API and the @mention mechanism.  
The goal was to verify that users can add, fetch, update, and delete comments on tickets, and that `@username` mentions are parsed, persisted, returned in comment responses, and re-evaluated when comments are updated.

Before running the comments and mentions tests, the same setup flow was used:
create ADMIN user, login as ADMIN, create DEVELOPER author, login as DEVELOPER, create an additional DEVELOPER user to be mentioned, create a project, and create a ticket.

The returned IDs and JWT tokens were stored as Postman collection variables.

---

Test 1 - Create ADMIN user

**Method** POST  
**URL** http://localhost:8080/users

**Result** 200 OK  
**Body** UserResponse returned.

Purpose: created an ADMIN user for setup and protected API access.

---

Test 2 - Login as ADMIN

**Method** POST  
**URL** http://localhost:8080/auth/login

**Result** 200 OK  
**Body** AuthTokenResponse returned.

Purpose: obtained an ADMIN JWT token for setup and verification requests.

---

Test 3 - Create DEVELOPER author

**Method** POST  
**URL** http://localhost:8080/users

**Result** 200 OK  
**Body** UserResponse returned.

Purpose: created the user who will be used as the comment author.

---

Test 4 - Login as DEVELOPER author

**Method** POST  
**URL** http://localhost:8080/auth/login

**Result** 200 OK  
**Body** AuthTokenResponse returned.

Purpose: obtained a DEVELOPER JWT token for comment requests.

---

Test 5 - Create mentioned user

**Method** POST  
**URL** http://localhost:8080/users

**Result** 200 OK  
**Body** UserResponse returned.

Purpose: created a second DEVELOPER user that will be referenced by `@username` inside comment content.

---

Test 6 - Create project  
(using Bearer adminToken)

**Method** POST  
**URL** http://localhost:8080/projects

**Result** 200 OK  
**Body** ProjectResponse returned.

Purpose: created a project to contain the ticket used for comments.

---

Test 7 - Create ticket  
(using Bearer adminToken)

**Method** POST  
**URL** http://localhost:8080/tickets

**Result** 200 OK  
**Body** TicketResponse returned.

Purpose: created a ticket that comments can be attached to.

---

Test 8 - Add comment without mentions  
(using Bearer devToken)

**Method** POST  
**URL** http://localhost:8080/tickets/{ticketId}/comments

**Result** 200 OK  
**Body** CommentResponse returned with an empty `mentionedUsers` list.

Purpose: verified that a normal comment can be added to an existing non-deleted ticket.

---

Test 9 - Get comments for ticket  
(using Bearer adminToken)

**Method** GET  
**URL** http://localhost:8080/tickets/{ticketId}/comments

**Result** 200 OK  
**Body** List of CommentResponse objects returned. The created comment appeared in the list.

Purpose: verified that comments can be fetched for a ticket.

---

Test 10 - Add comment with case-insensitive mention  
(using Bearer devToken)

**Method** POST  
**URL** http://localhost:8080/tickets/{ticketId}/comments

**Result** 200 OK  
**Body** CommentResponse returned with the mentioned user inside `mentionedUsers`.

Purpose: verified that `@username` mentions are matched case-insensitively.

---

Test 11 - Add comment with duplicate mentions  
(using Bearer devToken)

**Method** POST  
**URL** http://localhost:8080/tickets/{ticketId}/comments

**Result** 200 OK  
**Body** CommentResponse returned with the mentioned user appearing only once.

Purpose: verified that duplicate mentions of the same user in one comment do not create duplicate mention associations.

---

Test 12 - Add comment with unknown mention  
(using Bearer devToken)

**Method** POST  
**URL** http://localhost:8080/tickets/{ticketId}/comments

**Result** 200 OK  
**Body** CommentResponse returned without the unknown username in `mentionedUsers`.

Purpose: verified that unknown `@username` values are ignored and do not fail comment creation.

---

Test 13 - Update comment to remove mention  
(using Bearer devToken)

**Method** PATCH  
**URL** http://localhost:8080/tickets/{ticketId}/comments/{commentId}

**Result** 200 OK  
**Body** empty

Purpose: updated a comment that previously mentioned a user and removed the mention from the content.

---

Test 14 - Verify mention was removed after update  
(using Bearer adminToken)

**Method** GET  
**URL** http://localhost:8080/tickets/{ticketId}/comments

**Result** 200 OK  
**Body** Updated comment appeared with an empty `mentionedUsers` list.

Purpose: verified that comment update re-evaluates mentions and removes old mention associations.

---

Test 15 - Update comment to add mention again  
(using Bearer devToken)

**Method** PATCH  
**URL** http://localhost:8080/tickets/{ticketId}/comments/{commentId}

**Result** 200 OK  
**Body** empty

Purpose: updated the same comment again and added the `@username` mention back.

---

Test 16 - Verify mention was added again  
(using Bearer adminToken)

**Method** GET  
**URL** http://localhost:8080/tickets/{ticketId}/comments

**Result** 200 OK  
**Body** Updated comment appeared with the mentioned user inside `mentionedUsers`.

Purpose: verified that comment update creates new mention associations after re-parsing the updated content.

---

Test 17 - Get user mentions  
(using Bearer adminToken)

**Method** GET  
**URL** http://localhost:8080/users/{mentionedUserId}/mentions?page=0&pageSize=20

**Result** 200 OK  
**Body** MentionsPageResponse returned, containing comments where the user was mentioned.

Purpose: verified that the system can retrieve comments where a specific user was mentioned.

---

Test 18 - Delete comment  
(using Bearer devToken)

**Method** DELETE  
**URL** http://localhost:8080/tickets/{ticketId}/comments/{commentId}

**Result** 200 OK  
**Body** empty

Purpose: verified that a comment can be deleted.

---

Test 19 - Verify deleted comment no longer appears  
(using Bearer adminToken)

**Method** GET  
**URL** http://localhost:8080/tickets/{ticketId}/comments

**Result** 200 OK  
**Body** Deleted comment did not appear in the comments list.

Purpose: verified that deleted comments are removed from the ticket comments response.

---

Test 20 - Add comment to missing ticket should fail  
(using Bearer devToken)

**Method** POST  
**URL** http://localhost:8080/tickets/999999/comments

**Result** 404 Not Found

Purpose: verified that comments cannot be added to a non-existing ticket.

---

Test 21 - Add comment with missing author should fail  
(using Bearer devToken)

**Method** POST  
**URL** http://localhost:8080/tickets/{ticketId}/comments

**Result** 404 Not Found

Purpose: verified that comments cannot be added with a non-existing authorId.

---

Test 22 - Access comments without JWT token

**Method** GET  
**URL** http://localhost:8080/tickets/{ticketId}/comments

**Result** 401 Unauthorized

Purpose: verified that comment endpoints are protected by JWT authentication.

---

Stage Four Result:

**Postman collection result:** 34 passed checks.  
**Maven test result:** 35 tests passed, 0 failures, 0 errors.

This stage verified:
- JWT protection for comment and mention endpoints.
- Adding comments to existing tickets.
- Fetching comments for a ticket.
- Updating comment content.
- Deleting comments.
- Validation for missing tickets.
- Validation for missing authors.
- Case-insensitive `@username` mention parsing.
- Deduplication of repeated mentions in the same comment.
- Ignoring unknown mentioned usernames.
- Mention persistence.
- Mention metadata included in comment responses.
- Mention re-evaluation on comment update.
- User mention retrieval through `/users/{userId}/mentions`.
- Comment state-changing actions recorded through audit logging.


## 5. Stage Five - Ticket Dependencies API

Stage Five focused on the Ticket Dependencies API.  
The goal was to verify that tickets can depend on other tickets, that dependency rules are enforced correctly, and that unresolved blockers prevent a ticket from transitioning to `DONE`.

Before running the dependency tests, the same setup flow was used:
create ADMIN user, login as ADMIN, create DEVELOPER user, login as DEVELOPER, create a project, and create the required tickets.

The returned IDs and JWT tokens were stored as Postman collection variables.

---

Test 1 - Create ADMIN user

**Method** POST  
**URL** http://localhost:8080/users

**Result** 200 OK  
**Body** UserResponse returned.

Purpose: created an ADMIN user for setup and protected API access.

---

Test 2 - Login as ADMIN

**Method** POST  
**URL** http://localhost:8080/auth/login

**Result** 200 OK  
**Body** AuthTokenResponse returned.

Purpose: obtained an ADMIN JWT token for setup and dependency API requests.

---

Test 3 - Create DEVELOPER user

**Method** POST  
**URL** http://localhost:8080/users

**Result** 200 OK  
**Body** UserResponse returned.

Purpose: created a DEVELOPER user to use as ticket assignee and project owner.

---

Test 4 - Login as DEVELOPER

**Method** POST  
**URL** http://localhost:8080/auth/login

**Result** 200 OK  
**Body** AuthTokenResponse returned.

Purpose: obtained a DEVELOPER JWT token for authorization checks.

---

Test 5 - Create main project  
(using Bearer adminToken)

**Method** POST  
**URL** http://localhost:8080/projects

**Result** 200 OK  
**Body** ProjectResponse returned.

Purpose: created the project that contains the dependency test tickets.

---

Test 6 - Create blocker ticket A  
(using Bearer adminToken)

**Method** POST  
**URL** http://localhost:8080/tickets

**Result** 200 OK  
**Body** TicketResponse returned.

Purpose: created the blocker ticket. This ticket will block another ticket from being completed.

---

Test 7 - Create blocked ticket B  
(using Bearer adminToken)

**Method** POST  
**URL** http://localhost:8080/tickets

**Result** 200 OK  
**Body** TicketResponse returned.

Purpose: created the ticket that will depend on blocker ticket A.

---

Test 8 - Add dependency: ticket B blocked by ticket A  
(using Bearer adminToken)

**Method** POST  
**URL** http://localhost:8080/tickets/{blockedTicketId}/dependencies

**Result** 200 OK  
**Body** DependencyResponse returned.

Purpose: verified that a dependency can be created between two tickets in the same project.

---

Test 9 - List dependencies for ticket B  
(using Bearer adminToken)

**Method** GET  
**URL** http://localhost:8080/tickets/{blockedTicketId}/dependencies

**Result** 200 OK  
**Body** List of dependencies returned. The blocker ticket A appeared in the list.

Purpose: verified that dependencies can be retrieved for a ticket.

---

Test 10 - Duplicate dependency should fail  
(using Bearer adminToken)

**Method** POST  
**URL** http://localhost:8080/tickets/{blockedTicketId}/dependencies

**Result** 409 Conflict

Purpose: verified that the same dependency cannot be added twice.

---

Test 11 - Self-dependency should fail  
(using Bearer adminToken)

**Method** POST  
**URL** http://localhost:8080/tickets/{blockerTicketId}/dependencies

**Result** 400 Bad Request or 409 Conflict

Purpose: verified that a ticket cannot depend on itself.

---

Test 12 - Blocked ticket cannot move to DONE while blocker is unresolved  
(using Bearer adminToken)

**Method** PATCH  
**URL** http://localhost:8080/tickets/{blockedTicketId}

**Result** 409 Conflict

Purpose: verified the core dependency rule: a ticket cannot transition to `DONE` while at least one blocker is still unresolved.

---

Test 13 - Move blocker ticket A to DONE  
(using Bearer adminToken)

**Method** PATCH  
**URL** http://localhost:8080/tickets/{blockerTicketId}

**Result** 200 OK

Purpose: resolved the blocker ticket so that the blocked ticket can later be completed.

---

Test 14 - Blocked ticket B can now move to DONE  
(using Bearer adminToken)

**Method** PATCH  
**URL** http://localhost:8080/tickets/{blockedTicketId}

**Result** 200 OK

Purpose: verified that once all blockers are resolved, the blocked ticket is allowed to transition to `DONE`.

---

Test 15 - Create remove-test blocker ticket  
(using Bearer adminToken)

**Method** POST  
**URL** http://localhost:8080/tickets

**Result** 200 OK  
**Body** TicketResponse returned.

Purpose: created a separate blocker ticket for testing dependency removal.

---

Test 16 - Create remove-test blocked ticket  
(using Bearer adminToken)

**Method** POST  
**URL** http://localhost:8080/tickets

**Result** 200 OK  
**Body** TicketResponse returned.

Purpose: created a separate blocked ticket for testing dependency removal.

---

Test 17 - Add dependency for removal test  
(using Bearer adminToken)

**Method** POST  
**URL** http://localhost:8080/tickets/{removeBlockedTicketId}/dependencies

**Result** 200 OK

Purpose: created a dependency that will later be removed.

---

Test 18 - Remove dependency  
(using Bearer adminToken)

**Method** DELETE  
**URL** http://localhost:8080/tickets/{removeBlockedTicketId}/dependencies/{removeBlockerTicketId}

**Result** 200 OK  
**Body** empty

Purpose: verified that an existing dependency can be removed.

---

Test 19 - Verify dependency was removed  
(using Bearer adminToken)

**Method** GET  
**URL** http://localhost:8080/tickets/{removeBlockedTicketId}/dependencies

**Result** 200 OK  
**Body** Removed blocker no longer appeared in the dependency list.

Purpose: verified that dependency removal was persisted correctly.

---

Test 20 - Create second project  
(using Bearer adminToken)

**Method** POST  
**URL** http://localhost:8080/projects

**Result** 200 OK  
**Body** ProjectResponse returned.

Purpose: created a second project for testing cross-project dependency validation.

---

Test 21 - Create ticket in second project  
(using Bearer adminToken)

**Method** POST  
**URL** http://localhost:8080/tickets

**Result** 200 OK  
**Body** TicketResponse returned.

Purpose: created a ticket from another project to use in the cross-project dependency test.

---

Test 22 - Cross-project dependency should fail  
(using Bearer adminToken)

**Method** POST  
**URL** http://localhost:8080/tickets/{removeBlockedTicketId}/dependencies

**Result** 400 Bad Request or 409 Conflict

Purpose: verified that a ticket cannot depend on a blocker ticket from a different project.

---

Test 23 - Missing blocker should fail  
(using Bearer adminToken)

**Method** POST  
**URL** http://localhost:8080/tickets/{removeBlockedTicketId}/dependencies

**Result** 404 Not Found

Purpose: verified that adding a dependency fails when the blocker ticket does not exist.

---

Test 24 - Access dependencies without JWT token

**Method** GET  
**URL** http://localhost:8080/tickets/{removeBlockedTicketId}/dependencies

**Result** 401 Unauthorized

Purpose: verified that dependency endpoints are protected by JWT authentication.

---

Stage Five Result:

**Postman collection result:** 27 passed checks.  
**Maven test result:** 45 tests passed, 0 failures, 0 errors.

This stage verified:
- JWT protection for dependency endpoints.
- Adding dependencies between tickets in the same project.
- Listing ticket dependencies.
- Removing ticket dependencies.
- Rejection of duplicate dependencies.
- Rejection of self-dependencies.
- Rejection of cross-project dependencies.
- Rejection of missing blocker tickets.
- Enforcement of the unresolved-blocker rule when transitioning a ticket to `DONE`.
- Allowing transition to `DONE` after all blockers are resolved.
- Dependency add/remove actions recorded through audit logging.





## 6. Stage Six - Auto-Assignment and Project Workload

Stage Six focused on automatic ticket assignment and project workload reporting.  
The goal was to verify that tickets without an explicit assignee are automatically assigned to the least-loaded `DEVELOPER`, that `ADMIN` users are excluded from auto-assignment, and that project workload is calculated correctly.

Before running the auto-assignment and workload tests, the same setup flow was used:
create ADMIN user, login as ADMIN, create multiple DEVELOPER users, create a project, and create tickets with and without explicit assignees.

The returned IDs and JWT tokens were stored as Postman collection variables.

---

Test 1 - Create ADMIN user

**Method** POST  
**URL** http://localhost:8080/users

**Result** 200 OK  
**Body** UserResponse returned.

Purpose: created an ADMIN user for setup and protected API access.

---

Test 2 - Login as ADMIN

**Method** POST  
**URL** http://localhost:8080/auth/login

**Result** 200 OK  
**Body** AuthTokenResponse returned.

Purpose: obtained an ADMIN JWT token for setup and workload API requests.

---

Test 3 - Create DEVELOPER 1

**Method** POST  
**URL** http://localhost:8080/users

**Result** 200 OK  
**Body** UserResponse returned.

Purpose: created the first developer, later used as an explicit ticket assignee.

---

Test 4 - Create DEVELOPER 2

**Method** POST  
**URL** http://localhost:8080/users

**Result** 200 OK  
**Body** UserResponse returned.

Purpose: created a second developer for auto-assignment candidate selection.

---

Test 5 - Create DEVELOPER 3

**Method** POST  
**URL** http://localhost:8080/users

**Result** 200 OK  
**Body** UserResponse returned.

Purpose: created a third developer for auto-assignment candidate selection and tie-handling checks.

---

Test 6 - Create project  
(using Bearer adminToken)

**Method** POST  
**URL** http://localhost:8080/projects

**Result** 200 OK  
**Body** ProjectResponse returned.

Purpose: created the project used for workload and auto-assignment testing.

---

Test 7 - Create ticket with explicit assignee  
(using Bearer adminToken)

**Method** POST  
**URL** http://localhost:8080/tickets

**Result** 200 OK  
**Body** TicketResponse returned with assigneeId equal to DEVELOPER 1.

Purpose: verified that when `assigneeId` is explicitly provided, the system keeps the provided assignee and does not auto-assign to someone else.

---

Test 8 - Create ticket without assignee  
(using Bearer adminToken)

**Method** POST  
**URL** http://localhost:8080/tickets

**Result** 200 OK  
**Body** TicketResponse returned with an automatically selected assigneeId.

Purpose: verified that a ticket without an explicit assignee triggers auto-assignment.

Expected behavior:
- The ticket was not assigned to the ADMIN user.
- The ticket was not assigned to the already-loaded DEVELOPER 1.
- The ticket was assigned to one of the least-loaded DEVELOPER users.

---

Test 9 - Get project workload  
(using Bearer adminToken)

**Method** GET  
**URL** http://localhost:8080/projects/{projectId}/workload

**Result** 200 OK  
**Body** WorkloadResponse list returned.

Purpose: verified that the workload endpoint returns workload information for DEVELOPER users in the project.

This test also verified:
- ADMIN users are excluded from the workload response.
- DEVELOPER users are included.
- Workload counts are based on non-DONE, non-deleted tickets assigned in the project.

---

Test 10 - PATCH explicit assignee override  
(using Bearer adminToken)

**Method** PATCH  
**URL** http://localhost:8080/tickets/{ticketId}

**Result** 200 OK  
**Body** empty

Purpose: verified that explicit assignment through ticket update overrides the current assignee and does not trigger auto-assignment.

---

Test 11 - Verify PATCH explicit assignee  
(using Bearer adminToken)

**Method** GET  
**URL** http://localhost:8080/tickets/{ticketId}

**Result** 200 OK  
**Body** TicketResponse returned with assigneeId equal to the explicitly assigned DEVELOPER.

Purpose: verified that the explicit assignment update was persisted correctly.

---

Test 12 - Move ticket to DONE  
(using Bearer adminToken)

**Method** PATCH  
**URL** http://localhost:8080/tickets/{ticketId}

**Result** 200 OK  
**Body** empty

Purpose: moved a ticket to `DONE` in order to verify that DONE tickets are excluded from workload calculations.

---

Test 13 - Verify workload excludes DONE tickets  
(using Bearer adminToken)

**Method** GET  
**URL** http://localhost:8080/projects/{projectId}/workload

**Result** 200 OK  
**Body** WorkloadResponse list returned.

Purpose: verified that tickets with status `DONE` are not counted as open workload.

---

Test 14 - Access workload without JWT token

**Method** GET  
**URL** http://localhost:8080/projects/{projectId}/workload

**Result** 401 Unauthorized

Purpose: verified that the workload endpoint is protected by JWT authentication.

---

Stage Six Result:

**Postman collection result:** 24 passed checks.  
**Maven test result:** 55 tests passed, 0 failures, 0 errors.

This stage verified:
- JWT protection for the workload endpoint.
- Auto-assignment on ticket creation when `assigneeId` is missing.
- Explicit assignee preservation when `assigneeId` is provided during ticket creation.
- Explicit assignee override through ticket update.
- ADMIN users are excluded from auto-assignment.
- DEVELOPER users are used as auto-assignment candidates.
- Least-loaded developer selection.
- Workload calculation per project.
- Exclusion of DONE tickets from workload.
- Exclusion of deleted tickets from workload through the implemented service logic.
- AUTO_ASSIGN audit logging with SYSTEM actor.


## 7. Stage Seven - CSV Import and Export

Stage Seven focused on ticket CSV import and export.  
The goal was to verify that tickets can be exported to a valid CSV format, that CSV escaping works correctly for commas and quotes, and that ticket import can process valid rows while reporting invalid rows without aborting the whole import.

Before running the CSV tests, the setup flow was used:
create ADMIN user, login as ADMIN, create DEVELOPER user, create a project, and create tickets for export.

The returned IDs and JWT tokens were stored as Postman collection variables.

---

Test 1 - Create ADMIN user

**Method** POST  
**URL** http://localhost:8080/users

**Result** 200 OK  
**Body** UserResponse returned.

Purpose: created an ADMIN user for setup and protected API access.

---

Test 2 - Login as ADMIN

**Method** POST  
**URL** http://localhost:8080/auth/login

**Result** 200 OK  
**Body** AuthTokenResponse returned.

Purpose: obtained an ADMIN JWT token for CSV import/export requests.

---

Test 3 - Create DEVELOPER user

**Method** POST  
**URL** http://localhost:8080/users

**Result** 200 OK  
**Body** UserResponse returned.

Purpose: created a DEVELOPER user to use as project owner and ticket assignee.

---

Test 4 - Create project  
(using Bearer adminToken)

**Method** POST  
**URL** http://localhost:8080/projects

**Result** 200 OK  
**Body** ProjectResponse returned.

Purpose: created the project used for CSV export and import tests.

---

Test 5 - Create ticket with comma and quotes  
(using Bearer adminToken)

**Method** POST  
**URL** http://localhost:8080/tickets

**Result** 200 OK  
**Body** TicketResponse returned.

Purpose: created a ticket containing a comma and quotes in the title/description, in order to verify correct CSV escaping during export.

---

Test 6 - Create normal ticket  
(using Bearer adminToken)

**Method** POST  
**URL** http://localhost:8080/tickets

**Result** 200 OK  
**Body** TicketResponse returned.

Purpose: created a normal ticket as a simple control row for export.

---

Test 7 - Export tickets as CSV  
(using Bearer adminToken)

**Method** GET  
**URL** http://localhost:8080/tickets/export?projectId={projectId}

**Result** 200 OK  
**Body** CSV content returned.

Purpose: verified that the export endpoint returns CSV content for non-deleted tickets in the selected project.

This test verified:
- The response content type is `text/csv`.
- The CSV header is exactly:
  `id,title,description,status,priority,type,assigneeId`
- Exported tickets appear in the CSV.
- Fields containing commas are quoted.
- Quotes inside fields are escaped correctly by doubling them.

---

Test 8 - Import valid CSV file  
(using Bearer adminToken)

**Method** POST  
**URL** http://localhost:8080/tickets/import

**Request type** multipart/form-data  
**Fields**
- `projectId`
- `file`

**Result** 200 OK  
**Body** ImportTicketsResponse returned.

Expected result:
- `created = 2`
- `failed = 0`
- `errors = []`

Purpose: verified that a valid CSV file creates tickets successfully.

---

Test 9 - Import mixed CSV file with valid and invalid rows  
(using Bearer adminToken)

**Method** POST  
**URL** http://localhost:8080/tickets/import

**Request type** multipart/form-data  
**Fields**
- `projectId`
- `file`

**Result** 200 OK  
**Body** ImportTicketsResponse returned.

Expected result:
- Valid rows were created.
- Invalid rows were counted as failed.
- Row-level error messages were returned.

Purpose: verified that CSV import does not abort the whole import when one row is invalid. Later valid rows are still processed.

---

Test 10 - Export after import  
(using Bearer adminToken)

**Method** GET  
**URL** http://localhost:8080/tickets/export?projectId={projectId}

**Result** 200 OK  
**Body** CSV content returned.

Purpose: verified that tickets created by the import process appear in a later CSV export.

---

Test 11 - Import without JWT token

**Method** POST  
**URL** http://localhost:8080/tickets/import

**Result** 401 Unauthorized

Purpose: verified that CSV import is protected by JWT authentication.

---

Test 12 - Export without JWT token

**Method** GET  
**URL** http://localhost:8080/tickets/export?projectId={projectId}

**Result** 401 Unauthorized

Purpose: verified that CSV export is protected by JWT authentication.

---

Stage Seven Result:

**Maven test result:** 62 tests passed, 0 failures, 0 errors.

This stage verified:
- JWT protection for CSV import/export endpoints.
- CSV export for non-deleted project tickets.
- Correct CSV header order.
- Correct escaping of commas and quotes.
- CSV import from multipart file upload.
- Import ignores CSV id and generates new database ids.
- Import creates valid rows.
- Import reports invalid rows without aborting the whole file.
- Import returns created, failed, and errors counts.
- Imported tickets appear in later exports.
- Import action is recorded through audit logging.





## 8. Stage Eight - Attachments API

Stage Eight focused on ticket attachment upload, listing, and deletion.  
The goal was to verify that files can be attached to tickets, that only allowed content types are accepted, that the 10 MB size limit is enforced, and that attachments are stored as metadata plus binary content in PostgreSQL.

Before running the attachment tests, the setup flow was used:
create ADMIN user, login as ADMIN, create DEVELOPER user, create a project, and create a ticket.

The returned IDs and JWT tokens were stored as Postman collection variables.

---

Test 1 - Create ADMIN user

**Method** POST  
**URL** http://localhost:8080/users

**Result** 200 OK  
**Body** UserResponse returned.

Purpose: created an ADMIN user for setup and protected API access.

---

Test 2 - Login as ADMIN

**Method** POST  
**URL** http://localhost:8080/auth/login

**Result** 200 OK  
**Body** AuthTokenResponse returned.

Purpose: obtained an ADMIN JWT token for attachment API requests.

---

Test 3 - Create DEVELOPER user

**Method** POST  
**URL** http://localhost:8080/users

**Result** 200 OK  
**Body** UserResponse returned.

Purpose: created a DEVELOPER user to use as project owner and ticket assignee.

---

Test 4 - Create project  
(using Bearer adminToken)

**Method** POST  
**URL** http://localhost:8080/projects

**Result** 200 OK  
**Body** ProjectResponse returned.

Purpose: created the project used for attachment tests.

---

Test 5 - Create ticket  
(using Bearer adminToken)

**Method** POST  
**URL** http://localhost:8080/tickets

**Result** 200 OK  
**Body** TicketResponse returned.

Purpose: created the ticket that attachments will be uploaded to.

---

Test 6 - Upload valid text attachment  
(using Bearer adminToken)

**Method** POST  
**URL** http://localhost:8080/tickets/{ticketId}/attachments

**Request type** multipart/form-data  
**Field**
- `file`

**Result** 200 OK  
**Body** AttachmentResponse returned.

Example returned fields:
- `id`
- `ticketId`
- `filename`
- `contentType`
- `sizeBytes`

Purpose: verified that a valid `text/plain` file can be uploaded successfully.

This test also verified:
- Attachment metadata is returned.
- Raw file content is not returned in the JSON response.
- The attachment is linked to the correct ticket.

---

Test 7 - List attachments for ticket  
(using Bearer adminToken)

**Method** GET  
**URL** http://localhost:8080/tickets/{ticketId}/attachments

**Result** 200 OK  
**Body** List of AttachmentResponse objects returned.

Purpose: verified that uploaded attachments can be listed for a ticket.

---

Test 8 - Upload unsupported content type  
(using Bearer adminToken)

**Method** POST  
**URL** http://localhost:8080/tickets/{ticketId}/attachments

**Request type** multipart/form-data  
**Field**
- `file`

**Result** 400 Bad Request

Purpose: verified that unsupported content types are rejected.

The tested rejected file had content type `text/html`.  
The API returned a clear error message listing the allowed content types:
- `image/png`
- `image/jpeg`
- `application/pdf`
- `text/plain`

---

Test 9 - Upload file larger than 10 MB  
(using Bearer adminToken)

**Method** POST  
**URL** http://localhost:8080/tickets/{ticketId}/attachments

**Request type** multipart/form-data  
**Field**
- `file`

**Result** 400 Bad Request

Purpose: verified that the API rejects files larger than the allowed 10 MB limit.

---

Test 10 - Upload attachment to missing ticket  
(using Bearer adminToken)

**Method** POST  
**URL** http://localhost:8080/tickets/999999/attachments

**Request type** multipart/form-data  
**Field**
- `file`

**Result** 404 Not Found

Purpose: verified that attachments cannot be uploaded to a non-existing ticket.

---

Test 11 - Delete attachment  
(using Bearer adminToken)

**Method** DELETE  
**URL** http://localhost:8080/tickets/{ticketId}/attachments/{attachmentId}

**Result** 200 OK  
**Body** empty

Purpose: verified that an existing attachment can be deleted.

---

Test 12 - Verify deleted attachment is gone  
(using Bearer adminToken)

**Method** GET  
**URL** http://localhost:8080/tickets/{ticketId}/attachments

**Result** 200 OK  
**Body** Empty list or list without the deleted attachment.

Purpose: verified that the deleted attachment no longer appears in the ticket attachment list.

---

Test 13 - Delete missing attachment  
(using Bearer adminToken)

**Method** DELETE  
**URL** http://localhost:8080/tickets/{ticketId}/attachments/999999

**Result** 404 Not Found

Purpose: verified that deleting a non-existing attachment returns a proper not-found response.

---

Test 14 - Access attachments without JWT token

**Method** GET  
**URL** http://localhost:8080/tickets/{ticketId}/attachments

**Result** 401 Unauthorized

Purpose: verified that attachment endpoints are protected by JWT authentication.

---

Stage Eight Result:

**Maven test result:** 72 tests passed, 0 failures, 0 errors.

This stage verified:
- JWT protection for attachment endpoints.
- Upload of valid attachments.
- Listing attachments for a ticket.
- Deletion of attachments.
- Rejection of unsupported content types.
- Rejection of files larger than 10 MB.
- Rejection of uploads to missing tickets.
- Rejection of deleting missing attachments.
- Attachment metadata is returned without raw file content.
- Attachment content is stored in PostgreSQL.
- Upload/delete attachment actions are recorded through audit logging.




## 9. Stage Nine - Auto Escalation Scheduler

Stage Nine focused on automatic overdue ticket escalation.  
The goal was to verify that the background scheduler detects overdue tickets, escalates their priority according to the required rules, ignores tickets that should not be escalated, and does not change ticket status.

Unlike most previous stages, this feature is mainly background-service based and does not expose a dedicated public API endpoint. Therefore, the manual Postman test used the existing Ticket API to create tickets with past/future `dueDate` values, waited for the scheduler to run, and then fetched the tickets again to verify the results.

Before running the escalation tests, the setup flow was used:
create ADMIN user, login as ADMIN, create DEVELOPER user, create a project, and create several tickets with different priorities and due dates.

The returned IDs and JWT tokens were stored as Postman collection variables.

Important manual testing note:  
The scheduler runs every 60 seconds with an initial delay. Therefore, the setup requests were executed first, then I waited at least 70 seconds before running the verification requests.

---

Test 1 - Create ADMIN user

**Method** POST  
**URL** http://localhost:8080/users

**Result** 200 OK  
**Body** UserResponse returned.

Purpose: created an ADMIN user for setup and protected API access.

---

Test 2 - Login as ADMIN

**Method** POST  
**URL** http://localhost:8080/auth/login

**Result** 200 OK  
**Body** AuthTokenResponse returned.

Purpose: obtained an ADMIN JWT token for ticket creation and verification requests.

---

Test 3 - Create DEVELOPER user

**Method** POST  
**URL** http://localhost:8080/users

**Result** 200 OK  
**Body** UserResponse returned.

Purpose: created a DEVELOPER user to use as project owner and ticket assignee.

---

Test 4 - Create project  
(using Bearer adminToken)

**Method** POST  
**URL** http://localhost:8080/projects

**Result** 200 OK  
**Body** ProjectResponse returned.

Purpose: created the project used for escalation tests.

---

Test 5 - Create overdue LOW ticket  
(using Bearer adminToken)

**Method** POST  
**URL** http://localhost:8080/tickets

**Result** 200 OK  
**Body** TicketResponse returned.

Purpose: created an overdue ticket with priority `LOW` and a past `dueDate`.

Expected after scheduler run:
- priority changes from `LOW` to `MEDIUM`
- status remains unchanged

---

Test 6 - Create overdue HIGH ticket  
(using Bearer adminToken)

**Method** POST  
**URL** http://localhost:8080/tickets

**Result** 200 OK  
**Body** TicketResponse returned.

Purpose: created an overdue ticket with priority `HIGH` and a past `dueDate`.

Expected after scheduler run:
- priority changes from `HIGH` to `CRITICAL`
- status remains unchanged

---

Test 7 - Create overdue CRITICAL ticket  
(using Bearer adminToken)

**Method** POST  
**URL** http://localhost:8080/tickets

**Result** 200 OK  
**Body** TicketResponse returned.

Purpose: created an overdue ticket that is already `CRITICAL`.

Expected after scheduler run:
- priority remains `CRITICAL`
- `isOverdue` becomes `true`
- status remains unchanged

---

Test 8 - Create overdue DONE ticket  
(using Bearer adminToken)

**Method** POST  
**URL** http://localhost:8080/tickets

**Result** 200 OK  
**Body** TicketResponse returned.

Purpose: created a ticket with status `DONE`, priority `LOW`, and a past `dueDate`.

Expected after scheduler run:
- ticket is ignored
- status remains `DONE`
- priority remains `LOW`

---

Test 9 - Create future due LOW ticket  
(using Bearer adminToken)

**Method** POST  
**URL** http://localhost:8080/tickets

**Result** 200 OK  
**Body** TicketResponse returned.

Purpose: created a ticket with priority `LOW` and a future `dueDate`.

Expected after scheduler run:
- ticket is ignored
- priority remains `LOW`
- `isOverdue` remains `false`

---

Manual wait step

After creating the setup tickets, I waited at least 70 seconds before running the verification requests.

Purpose: allowed the scheduled escalation job to run at least once.

The Spring Boot console also showed the scheduled query running against overdue non-DONE, non-deleted tickets.

---

Test 10 - Verify overdue LOW ticket was escalated  
(using Bearer adminToken)

**Method** GET  
**URL** http://localhost:8080/tickets/{lowTicketId}

**Result** 200 OK  
**Body** TicketResponse returned.

Expected result:
- priority is `MEDIUM`
- status is still unchanged

Purpose: verified `LOW -> MEDIUM` escalation.

---

Test 11 - Verify overdue HIGH ticket was escalated  
(using Bearer adminToken)

**Method** GET  
**URL** http://localhost:8080/tickets/{highTicketId}

**Result** 200 OK  
**Body** TicketResponse returned.

Expected result:
- priority is `CRITICAL`
- status is still unchanged

Purpose: verified `HIGH -> CRITICAL` escalation.

---

Test 12 - Verify overdue CRITICAL ticket is marked overdue  
(using Bearer adminToken)

**Method** GET  
**URL** http://localhost:8080/tickets/{criticalTicketId}

**Result** 200 OK  
**Body** TicketResponse returned.

Expected result:
- priority remains `CRITICAL`
- `isOverdue` is `true`
- status is still unchanged

Purpose: verified that already-critical overdue tickets are marked with `isOverdue=true`.

---

Test 13 - Verify DONE ticket was ignored  
(using Bearer adminToken)

**Method** GET  
**URL** http://localhost:8080/tickets/{doneTicketId}

**Result** 200 OK  
**Body** TicketResponse returned.

Expected result:
- status remains `DONE`
- priority remains `LOW`

Purpose: verified that DONE tickets are ignored by auto-escalation.

---

Test 14 - Verify future due ticket was ignored  
(using Bearer adminToken)

**Method** GET  
**URL** http://localhost:8080/tickets/{futureTicketId}

**Result** 200 OK  
**Body** TicketResponse returned.

Expected result:
- priority remains `LOW`
- `isOverdue` remains `false`

Purpose: verified that tickets whose due date has not passed are ignored.

---

Test 15 - Verify CRITICAL ticket remains idempotent after another scheduler run  
(using Bearer adminToken)

**Method** GET  
**URL** http://localhost:8080/tickets/{criticalTicketId}

**Result** 200 OK  
**Body** TicketResponse returned.

Manual note: before this verification, I waited for another scheduler cycle.

Expected result:
- priority remains `CRITICAL`
- `isOverdue` remains `true`

Purpose: verified that repeated scheduler runs do not escalate beyond `CRITICAL`.

---

Test 16 - Access ticket without JWT token

**Method** GET  
**URL** http://localhost:8080/tickets/{lowTicketId}

**Result** 401 Unauthorized

Purpose: verified that the normal ticket API used for escalation verification remains protected by JWT authentication.

---

Stage Nine Result:

**Maven test result:** 83 tests passed, 0 failures, 0 errors.  
**Manual Postman result:** escalation verification passed after waiting for the scheduled job to run.

This stage verified:
- Spring scheduling is enabled.
- The scheduler runs periodically.
- Overdue tickets are detected using `dueDate`.
- DONE tickets are ignored.
- Deleted tickets are ignored by service logic.
- Future due-date tickets are ignored.
- Priority escalation follows the required order:
    - `LOW -> MEDIUM`
    - `MEDIUM -> HIGH`
    - `HIGH -> CRITICAL`
- `CRITICAL` tickets are not escalated beyond `CRITICAL`.
- Overdue `CRITICAL` tickets are marked with `isOverdue=true`.
- Ticket status is not changed by escalation.
- AUTO_ESCALATE audit logging is recorded with SYSTEM actor.
- Repeated scheduler runs are idempotent for already-critical overdue tickets.



## 10. Stage Ten - Audit Log API

Stage Ten focused on the read-only Audit Log API.  
The goal was to verify that audit logs can be retrieved through the API, that optional filters work correctly, that results are returned newest first, and that invalid filter values return clear errors.

Before running the audit log tests, the setup flow was used:
create ADMIN user, login as ADMIN, create DEVELOPER user, create a project, and create a ticket.

Creating the project and ticket generated audit log records that were then queried through the Audit Log API.

The returned IDs and JWT token were stored as Postman collection variables.

---

Test 1 - Create ADMIN user

**Method** POST  
**URL** http://localhost:8080/users

**Result** 200 OK  
**Body** UserResponse returned.

Purpose: created an ADMIN user for setup and protected API access.

---

Test 2 - Login as ADMIN

**Method** POST  
**URL** http://localhost:8080/auth/login

**Result** 200 OK  
**Body** AuthTokenResponse returned.

Purpose: obtained an ADMIN JWT token for audit log API requests.

---

Test 3 - Create DEVELOPER user

**Method** POST  
**URL** http://localhost:8080/users

**Result** 200 OK  
**Body** UserResponse returned.

Purpose: created a DEVELOPER user to use as project owner and ticket assignee.

---

Test 4 - Create project to generate audit log

**Method** POST  
**URL** http://localhost:8080/projects

**Result** 200 OK  
**Body** ProjectResponse returned.

Purpose: generated a project-related audit log entry, such as `CREATE_PROJECT`.

---

Test 5 - Create ticket to generate audit log

**Method** POST  
**URL** http://localhost:8080/tickets

**Result** 200 OK  
**Body** TicketResponse returned.

Purpose: generated a ticket-related audit log entry, such as `CREATE_TICKET`.

---

Test 6 - Access audit logs without JWT token

**Method** GET  
**URL** http://localhost:8080/audit-logs

**Result** 401 Unauthorized

Purpose: verified that the Audit Log API is protected by JWT authentication.

---

Test 7 - Get all audit logs  
(using Bearer adminToken)

**Method** GET  
**URL** http://localhost:8080/audit-logs

**Result** 200 OK  
**Body** List of AuditLogResponse objects returned.

Purpose: verified that audit logs can be retrieved successfully.

This test also verified:
- The response is an array.
- At least one audit log exists.
- Audit log objects contain expected fields such as:
    - `id`
    - `action`
    - `entityType`
    - `entityId`
    - `actor`
    - `timestamp`
- Results are sorted newest first by timestamp.

---

Test 8 - Filter audit logs by entityType

**Method** GET  
**URL** http://localhost:8080/audit-logs?entityType=PROJECT

**Result** 200 OK  
**Body** List of AuditLogResponse objects returned.

Purpose: verified that filtering by `entityType` works.

Expected result:
- Every returned audit log has `entityType = PROJECT`.

---

Test 9 - Filter audit logs by entityId

**Method** GET  
**URL** http://localhost:8080/audit-logs?entityId={projectId}

**Result** 200 OK  
**Body** List of AuditLogResponse objects returned.

Purpose: verified that filtering by `entityId` works.

Expected result:
- Every returned audit log has `entityId` equal to the requested project id.

---

Test 10 - Filter audit logs by action

**Method** GET  
**URL** http://localhost:8080/audit-logs?action=CREATE_PROJECT

**Result** 200 OK  
**Body** List of AuditLogResponse objects returned.

Purpose: verified that filtering by `action` works.

Expected result:
- Every returned audit log has `action = CREATE_PROJECT`.

---

Test 11 - Filter audit logs by actor

**Method** GET  
**URL** http://localhost:8080/audit-logs?actor=USER

**Result** 200 OK  
**Body** List of AuditLogResponse objects returned.

Purpose: verified that filtering by `actor` works.

Expected result:
- Every returned audit log has `actor = USER`.

---

Test 12 - Combined audit log filters

**Method** GET  
**URL** http://localhost:8080/audit-logs?entityType=PROJECT&entityId={projectId}&action=CREATE_PROJECT&actor=USER

**Result** 200 OK  
**Body** List of AuditLogResponse objects returned.

Purpose: verified that multiple filters can be combined in the same request.

Expected result:
- Returned logs match all provided filters:
    - `entityType = PROJECT`
    - `entityId = {projectId}`
    - `action = CREATE_PROJECT`
    - `actor = USER`

---

Test 13 - Invalid entityType should fail

**Method** GET  
**URL** http://localhost:8080/audit-logs?entityType=NOT_A_TYPE

**Result** 400 Bad Request

Purpose: verified that invalid `entityType` query values are rejected with a clear client error.

---

Test 14 - Invalid action should fail

**Method** GET  
**URL** http://localhost:8080/audit-logs?action=NOT_AN_ACTION

**Result** 400 Bad Request

Purpose: verified that invalid `action` query values are rejected with a clear client error.

---

Test 15 - Invalid actor should fail

**Method** GET  
**URL** http://localhost:8080/audit-logs?actor=NOT_AN_ACTOR

**Result** 400 Bad Request

Purpose: verified that invalid `actor` query values are rejected with a clear client error.

---

Stage Ten Result:

**Postman collection result:** 25 passed checks.  
**Maven test result:** 94 tests passed, 0 failures, 0 errors.

This stage verified:
- JWT protection for the Audit Log API.
- Read-only retrieval of audit logs.
- Returning `AuditLogResponse` DTOs rather than entities.
- Newest-first sorting by timestamp.
- Filtering by `entityType`.
- Filtering by `entityId`.
- Filtering by `action`.
- Filtering by `actor`.
- Combined filtering using multiple query parameters.
- Clear `400 Bad Request` responses for invalid enum query parameters.
- No unrelated business behavior was changed by the Audit Log API implementation.






## 11. Stage Eleven - Final Smoke and Regression Test

Stage Eleven focused on a broad final smoke/regression validation across the completed IssueFlow backend.  
The goal was to verify that the main implemented features still work together after all project phases were completed.

Unlike the earlier stage-specific collections, this final collection was not intended to replace the detailed manual tests. Instead, it provided one broad end-to-end sanity check covering authentication, projects, tickets, comments, mentions, dependencies, workload, CSV import/export, attachments, audit logs, security, and the scheduler.

Before running the final smoke test, the application was started with PostgreSQL running, and the Postman collection was executed against the local backend.

Some requests required helper files:
- CSV import file
- text attachment file

These files were selected manually in Postman for the relevant multipart requests.

---

Test 1 - Create ADMIN user

**Method** POST  
**URL** http://localhost:8080/users

**Result** 200 OK  
**Body** UserResponse returned.

Purpose: created an ADMIN user for setup and protected API access.

---

Test 2 - Login as ADMIN

**Method** POST  
**URL** http://localhost:8080/auth/login

**Result** 200 OK  
**Body** AuthTokenResponse returned.

Purpose: obtained an ADMIN JWT token for protected requests.

---

Test 3 - Create DEVELOPER user

**Method** POST  
**URL** http://localhost:8080/users

**Result** 200 OK  
**Body** UserResponse returned.

Purpose: created a DEVELOPER user for ownership, assignment, comments, and tickets.

---

Test 4 - Login as DEVELOPER

**Method** POST  
**URL** http://localhost:8080/auth/login

**Result** 200 OK  
**Body** AuthTokenResponse returned.

Purpose: obtained a DEVELOPER JWT token for user-level actions.

---

Test 5 - Create mentioned DEVELOPER user

**Method** POST  
**URL** http://localhost:8080/users

**Result** 200 OK  
**Body** UserResponse returned.

Purpose: created a user to be referenced by the comment mention mechanism.

---

Test 6 - Create project

**Method** POST  
**URL** http://localhost:8080/projects

**Result** 200 OK  
**Body** ProjectResponse returned.

Purpose: created the project used by the final smoke test.

---

Test 7 - Create explicit ticket

**Method** POST  
**URL** http://localhost:8080/tickets

**Result** 200 OK  
**Body** TicketResponse returned.

Purpose: verified basic ticket creation with an explicit assignee.

---

Test 8 - Create unassigned ticket

**Method** POST  
**URL** http://localhost:8080/tickets

**Result** 200 OK  
**Body** TicketResponse returned.

Purpose: verified that a ticket without an explicit assignee triggers auto-assignment.

---

Test 9 - Get project workload

**Method** GET  
**URL** http://localhost:8080/projects/{projectId}/workload

**Result** 200 OK  
**Body** WorkloadResponse list returned.

Purpose: verified that workload calculation still works after ticket creation and auto-assignment.

---

Test 10 - Add comment with mention

**Method** POST  
**URL** http://localhost:8080/tickets/{ticketId}/comments

**Result** 200 OK  
**Body** CommentResponse returned with mentioned user data.

Purpose: verified that comments and case-insensitive mention parsing still work.

---

Test 11 - Get user mentions

**Method** GET  
**URL** http://localhost:8080/users/{mentionedUserId}/mentions?page=0&pageSize=20

**Result** 200 OK  
**Body** MentionsPageResponse returned.

Purpose: verified that user mention retrieval still works.

---

Test 12 - Create blocker ticket

**Method** POST  
**URL** http://localhost:8080/tickets

**Result** 200 OK  
**Body** TicketResponse returned.

Purpose: created a blocker ticket for dependency validation.

---

Test 13 - Create blocked ticket

**Method** POST  
**URL** http://localhost:8080/tickets

**Result** 200 OK  
**Body** TicketResponse returned.

Purpose: created a ticket that will depend on the blocker ticket.

---

Test 14 - Add dependency

**Method** POST  
**URL** http://localhost:8080/tickets/{blockedTicketId}/dependencies

**Result** 200 OK

Purpose: verified that dependencies can still be added.

---

Test 15 - Blocked ticket cannot become DONE

**Method** PATCH  
**URL** http://localhost:8080/tickets/{blockedTicketId}

**Result** 409 Conflict

Purpose: verified that unresolved dependencies still prevent transition to `DONE`.

---

Test 16 - Complete blocker ticket

**Method** PATCH  
**URL** http://localhost:8080/tickets/{blockerTicketId}

**Result** 200 OK

Purpose: resolved the blocker ticket.

---

Test 17 - Blocked ticket can become DONE after blocker is resolved

**Method** PATCH  
**URL** http://localhost:8080/tickets/{blockedTicketId}

**Result** 200 OK

Purpose: verified that a blocked ticket can transition to `DONE` after all blockers are resolved.

---

Test 18 - Export CSV

**Method** GET  
**URL** http://localhost:8080/tickets/export?projectId={projectId}

**Result** 200 OK  
**Body** CSV content returned.

Purpose: verified that CSV export still works and returns the expected header.

---

Test 19 - Import CSV file

**Method** POST  
**URL** http://localhost:8080/tickets/import

**Request type** multipart/form-data  
**Fields**
- `projectId`
- `file`

**Result** 200 OK  
**Body** ImportTicketsResponse returned.

Purpose: verified that CSV import still works with a valid file.

---

Test 20 - Upload attachment

**Method** POST  
**URL** http://localhost:8080/tickets/{ticketId}/attachments

**Request type** multipart/form-data  
**Field**
- `file`

**Result** 200 OK  
**Body** AttachmentResponse returned.

Purpose: verified that attachment upload still works.

---

Test 21 - List attachments

**Method** GET  
**URL** http://localhost:8080/tickets/{ticketId}/attachments

**Result** 200 OK  
**Body** AttachmentResponse list returned.

Purpose: verified that uploaded attachments can be listed.

---

Test 22 - Delete attachment

**Method** DELETE  
**URL** http://localhost:8080/tickets/{ticketId}/attachments/{attachmentId}

**Result** 200 OK  
**Body** empty.

Purpose: verified that attachment deletion still works.

---

Test 23 - Get audit logs

**Method** GET  
**URL** http://localhost:8080/audit-logs

**Result** 200 OK  
**Body** AuditLogResponse list returned.

Purpose: verified that audit log retrieval works after multiple state-changing actions.

---

Test 24 - Filter audit logs by actor

**Method** GET  
**URL** http://localhost:8080/audit-logs?actor=USER

**Result** 200 OK  
**Body** AuditLogResponse list returned.

Purpose: verified that audit log filtering still works.

---

Test 25 - Protected endpoint without token

**Method** GET  
**URL** http://localhost:8080/projects

**Result** 401 Unauthorized

Purpose: verified that JWT protection still applies to protected endpoints.

---

Test 26 - Create overdue LOW ticket for scheduler

**Method** POST  
**URL** http://localhost:8080/tickets

**Result** 200 OK  
**Body** TicketResponse returned.

Purpose: created a ticket with a past `dueDate` for scheduler verification.

---

Manual wait step

After creating the overdue ticket, I waited at least 70 seconds before verifying the ticket again.

Purpose: allowed the automatic escalation scheduler to run.

---

Test 27 - Verify overdue ticket was escalated

**Method** GET  
**URL** http://localhost:8080/tickets/{escalationTicketId}

**Result** 200 OK  
**Body** TicketResponse returned.

Expected result:
- priority changed from `LOW`
- status remained `TODO`

Purpose: verified that the background scheduler still works as part of the final regression flow.

---

Stage Eleven Result:

**Maven test result:** 111 tests passed, 0 failures, 0 errors.  
**Final Postman smoke/regression collection:** passed.

This final stage verified:
- Authentication and JWT protection.
- User creation and login.
- Project creation.
- Ticket creation with explicit assignee.
- Auto-assignment for unassigned tickets.
- Workload endpoint.
- Comments and mentions.
- Mention retrieval.
- Ticket dependencies.
- Dependency blocking rule for `DONE`.
- CSV export.
- CSV import.
- Attachment upload/list/delete.
- Audit Log API retrieval and filtering.
- Protected endpoints reject unauthenticated requests.
- Auto-escalation scheduler behavior after waiting for the scheduled job.