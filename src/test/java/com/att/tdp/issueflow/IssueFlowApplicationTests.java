package com.att.tdp.issueflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.att.tdp.issueflow.domain.User;
import com.att.tdp.issueflow.domain.enums.ActorType;
import com.att.tdp.issueflow.domain.enums.AuditAction;
import com.att.tdp.issueflow.domain.enums.AuditEntityType;
import com.att.tdp.issueflow.repository.AuditLogRepository;
import com.att.tdp.issueflow.repository.CommentRepository;
import com.att.tdp.issueflow.repository.MentionRepository;
import com.att.tdp.issueflow.repository.ProjectRepository;
import com.att.tdp.issueflow.repository.RevokedTokenRepository;
import com.att.tdp.issueflow.repository.TicketDependencyRepository;
import com.att.tdp.issueflow.repository.TicketRepository;
import com.att.tdp.issueflow.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class IssueFlowApplicationTests {

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final TicketRepository ticketRepository;
    private final CommentRepository commentRepository;
    private final MentionRepository mentionRepository;
    private final TicketDependencyRepository ticketDependencyRepository;
    private final AuditLogRepository auditLogRepository;
    private final RevokedTokenRepository revokedTokenRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    IssueFlowApplicationTests(
            MockMvc mockMvc,
            ObjectMapper objectMapper,
            UserRepository userRepository,
            ProjectRepository projectRepository,
            TicketRepository ticketRepository,
            CommentRepository commentRepository,
            MentionRepository mentionRepository,
            TicketDependencyRepository ticketDependencyRepository,
            AuditLogRepository auditLogRepository,
            RevokedTokenRepository revokedTokenRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.ticketRepository = ticketRepository;
        this.commentRepository = commentRepository;
        this.mentionRepository = mentionRepository;
        this.ticketDependencyRepository = ticketDependencyRepository;
        this.auditLogRepository = auditLogRepository;
        this.revokedTokenRepository = revokedTokenRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @BeforeEach
    void cleanDatabase() {
        auditLogRepository.deleteAll();
        mentionRepository.deleteAll();
        commentRepository.deleteAll();
        ticketDependencyRepository.deleteAll();
        ticketRepository.deleteAll();
        projectRepository.deleteAll();
        revokedTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void contextLoads() {
    }

    @Test
    void createUserWithoutPasswordStoresDefaultPasswordHash() throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "jdoe",
                                  "email": "jdoe@example.com",
                                  "fullName": "John Doe",
                                  "role": "DEVELOPER"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("jdoe"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(content().string(not(containsString("passwordHash"))));

        User user = userRepository.findByUsernameIgnoreCase("jdoe").orElseThrow();
        assertThat(user.getPasswordHash()).isNotBlank();
        assertThat(passwordEncoder.matches("secret", user.getPasswordHash())).isTrue();
    }

    @Test
    void createUserWithExplicitPasswordStoresExplicitPasswordHash() throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "asmith",
                                  "email": "asmith@example.com",
                                  "fullName": "Alice Smith",
                                  "role": "ADMIN",
                                  "password": "better-secret"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        User user = userRepository.findByUsernameIgnoreCase("asmith").orElseThrow();
        assertThat(passwordEncoder.matches("better-secret", user.getPasswordHash())).isTrue();
        assertThat(passwordEncoder.matches("secret", user.getPasswordHash())).isFalse();
    }

    @Test
    void duplicateUsernameOrEmailReturnsConflict() throws Exception {
        createUser("jdoe", "jdoe@example.com", null);

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "JDOE",
                                  "email": "other@example.com",
                                  "fullName": "Other User",
                                  "role": "DEVELOPER"
                                }
                                """))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "other",
                                  "email": "JDOE@example.com",
                                  "fullName": "Other User",
                                  "role": "DEVELOPER"
                                }
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void loginWithValidCredentialsReturnsJwt() throws Exception {
        createUser("jdoe", "jdoe@example.com", "secret");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "jdoe",
                                  "password": "secret"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600));
    }

    @Test
    void loginWithInvalidCredentialsFails() throws Exception {
        createUser("jdoe", "jdoe@example.com", "secret");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "jdoe",
                                  "password": "wrong"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointRejectsMissingToken() throws Exception {
        mockMvc.perform(get("/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void revokedTokenIsRejectedAfterLogout() throws Exception {
        createUser("jdoe", "jdoe@example.com", "secret");
        String accessToken = login("jdoe", "secret");

        mockMvc.perform(post("/auth/logout")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/users")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createProjectWithValidOwnerSucceeds() throws Exception {
        User owner = createUser("owner", "owner@example.com", "secret");
        String token = login("owner", "secret");

        mockMvc.perform(post("/projects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Sample Project",
                                  "description": "A sample project",
                                  "ownerId": %d
                                }
                                """.formatted(owner.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Sample Project"))
                .andExpect(jsonPath("$.ownerId").value(owner.getId()));
    }

    @Test
    void createProjectWithMissingOwnerFails() throws Exception {
        createUser("jdoe", "jdoe@example.com", "secret");
        String token = login("jdoe", "secret");

        mockMvc.perform(post("/projects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Sample Project",
                                  "description": "A sample project",
                                  "ownerId": 9999
                                }
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void normalProjectGetHidesDeletedProject() throws Exception {
        User owner = createUser("owner", "owner@example.com", "secret");
        String token = login("owner", "secret");
        long projectId = createProject(token, owner.getId(), "Hidden Project");

        mockMvc.perform(get("/projects/{projectId}", projectId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/projects/{projectId}", projectId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/projects")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(get("/projects/{projectId}", projectId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void deletedProjectsEndpointRequiresAdmin() throws Exception {
        createUser("dev", "dev@example.com", "secret");
        String developerToken = login("dev", "secret");

        mockMvc.perform(get("/projects/deleted")
                        .header("Authorization", "Bearer " + developerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void restoreProjectEndpointRequiresAdmin() throws Exception {
        User owner = createUser("owner", "owner@example.com", "secret");
        String ownerToken = login("owner", "secret");
        long projectId = createProject(ownerToken, owner.getId(), "Restore Project");

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/projects/{projectId}", projectId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/projects/{projectId}/restore", projectId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void projectStateChangesWriteAuditLogs() throws Exception {
        User admin = createUser("admin", "admin@example.com", "secret", "ADMIN");
        String adminToken = login("admin", "secret");
        long projectId = createProject(adminToken, admin.getId(), "Audited Project");

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/projects/{projectId}", projectId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Audited Project Updated"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/projects/{projectId}", projectId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/projects/{projectId}/restore", projectId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(projectId));

        List<AuditAction> actions = auditLogRepository
                .findByEntityTypeAndEntityIdOrderByTimestampDesc(AuditEntityType.PROJECT, projectId)
                .stream()
                .map(log -> log.getAction())
                .toList();

        assertThat(actions).contains(
                AuditAction.CREATE_PROJECT,
                AuditAction.UPDATE_PROJECT,
                AuditAction.DELETE_PROJECT,
                AuditAction.RESTORE_PROJECT
        );
    }

    @Test
    void createTicketWithValidProjectSucceeds() throws Exception {
        User owner = createUser("owner", "owner@example.com", "secret");
        User assignee = createUser("dev", "dev@example.com", "secret");
        String token = login("owner", "secret");
        long projectId = createProject(token, owner.getId(), "Ticket Project");

        mockMvc.perform(post("/tickets")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Fix login bug",
                                  "description": "Login fails for valid users",
                                  "status": "TODO",
                                  "priority": "HIGH",
                                  "type": "BUG",
                                  "projectId": %d,
                                  "assigneeId": %d,
                                  "dueDate": "2026-04-01T00:00:00Z"
                                }
                                """.formatted(projectId, assignee.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Fix login bug"))
                .andExpect(jsonPath("$.projectId").value(projectId))
                .andExpect(jsonPath("$.assigneeId").value(assignee.getId()))
                .andExpect(jsonPath("$.dueDate").value("2026-04-01T00:00:00Z"))
                .andExpect(jsonPath("$.isOverdue").value(false));
    }

    @Test
    void createTicketWithMissingProjectFails() throws Exception {
        createUser("dev", "dev@example.com", "secret");
        String token = login("dev", "secret");

        mockMvc.perform(post("/tickets")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Missing project",
                                  "description": "No project exists",
                                  "status": "TODO",
                                  "priority": "HIGH",
                                  "type": "BUG",
                                  "projectId": 9999
                                }
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void createTicketWithMissingAssigneeFails() throws Exception {
        User owner = createUser("owner", "owner@example.com", "secret");
        String token = login("owner", "secret");
        long projectId = createProject(token, owner.getId(), "Ticket Project");

        mockMvc.perform(post("/tickets")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Missing assignee",
                                  "description": "No assignee exists",
                                  "status": "TODO",
                                  "priority": "MEDIUM",
                                  "type": "FEATURE",
                                  "projectId": %d,
                                  "assigneeId": 9999
                                }
                                """.formatted(projectId)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getTicketsByProjectHidesDeletedTickets() throws Exception {
        User owner = createUser("owner", "owner@example.com", "secret");
        String token = login("owner", "secret");
        long projectId = createProject(token, owner.getId(), "Visible Tickets");
        long hiddenTicketId = createTicket(token, projectId, owner.getId(), "TODO");
        long visibleTicketId = createTicket(token, projectId, owner.getId(), "TODO");

        mockMvc.perform(delete("/tickets/{ticketId}", hiddenTicketId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/tickets")
                        .param("projectId", String.valueOf(projectId))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(visibleTicketId));
    }

    @Test
    void deleteTicketSoftDeletesIt() throws Exception {
        User owner = createUser("owner", "owner@example.com", "secret");
        String token = login("owner", "secret");
        long projectId = createProject(token, owner.getId(), "Soft Delete Tickets");
        long ticketId = createTicket(token, projectId, owner.getId(), "TODO");

        mockMvc.perform(delete("/tickets/{ticketId}", ticketId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        assertThat(ticketRepository.findById(ticketId).orElseThrow().isDeleted()).isTrue();
        assertThat(ticketRepository.findById(ticketId).orElseThrow().getDeletedAt()).isNotNull();
    }

    @Test
    void normalTicketGetAfterDeleteReturnsNotFound() throws Exception {
        User owner = createUser("owner", "owner@example.com", "secret");
        String token = login("owner", "secret");
        long projectId = createProject(token, owner.getId(), "Hidden Ticket");
        long ticketId = createTicket(token, projectId, owner.getId(), "TODO");

        mockMvc.perform(delete("/tickets/{ticketId}", ticketId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/tickets/{ticketId}", ticketId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void deletedTicketsEndpointRequiresAdmin() throws Exception {
        User owner = createUser("owner", "owner@example.com", "secret");
        String token = login("owner", "secret");
        long projectId = createProject(token, owner.getId(), "Deleted Tickets");

        mockMvc.perform(get("/tickets/deleted")
                        .param("projectId", String.valueOf(projectId))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void restoreTicketEndpointRequiresAdmin() throws Exception {
        User owner = createUser("owner", "owner@example.com", "secret");
        String token = login("owner", "secret");
        long projectId = createProject(token, owner.getId(), "Restore Tickets");
        long ticketId = createTicket(token, projectId, owner.getId(), "TODO");

        mockMvc.perform(delete("/tickets/{ticketId}", ticketId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(post("/tickets/{ticketId}/restore", ticketId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void cannotUpdateTicketOnceDone() throws Exception {
        User owner = createUser("owner", "owner@example.com", "secret");
        String token = login("owner", "secret");
        long projectId = createProject(token, owner.getId(), "Done Tickets");
        long ticketId = createTicket(token, projectId, owner.getId(), "DONE");

        mockMvc.perform(patch("/tickets/{ticketId}", ticketId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Should not change"
                                }
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void cannotMoveTicketStatusBackward() throws Exception {
        User owner = createUser("owner", "owner@example.com", "secret");
        String token = login("owner", "secret");
        long projectId = createProject(token, owner.getId(), "Lifecycle Tickets");
        long ticketId = createTicket(token, projectId, owner.getId(), "IN_REVIEW");

        mockMvc.perform(patch("/tickets/{ticketId}", ticketId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "IN_PROGRESS"
                                }
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void ticketStateChangingActionsWriteAuditLog() throws Exception {
        User admin = createUser("admin", "admin@example.com", "secret", "ADMIN");
        String adminToken = login("admin", "secret");
        long projectId = createProject(adminToken, admin.getId(), "Audited Tickets");
        long ticketId = createTicket(adminToken, projectId, admin.getId(), "TODO");

        mockMvc.perform(patch("/tickets/{ticketId}", ticketId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "IN_PROGRESS",
                                  "priority": "CRITICAL"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/tickets/{ticketId}", ticketId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/tickets/{ticketId}/restore", ticketId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ticketId));

        List<AuditAction> actions = auditLogRepository
                .findByEntityTypeAndEntityIdOrderByTimestampDesc(AuditEntityType.TICKET, ticketId)
                .stream()
                .map(log -> log.getAction())
                .toList();

        assertThat(actions).contains(
                AuditAction.CREATE_TICKET,
                AuditAction.UPDATE_TICKET,
                AuditAction.DELETE_TICKET,
                AuditAction.RESTORE_TICKET
        );
    }

    @Test
    void addCommentToExistingTicketSucceeds() throws Exception {
        User author = createUser("author", "author@example.com", "secret");
        String token = login("author", "secret");
        long projectId = createProject(token, author.getId(), "Comment Project");
        long ticketId = createTicket(token, projectId, author.getId(), "TODO");

        mockMvc.perform(post("/tickets/{ticketId}/comments", ticketId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "authorId": %d,
                                  "content": "First comment"
                                }
                                """.formatted(author.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticketId").value(ticketId))
                .andExpect(jsonPath("$.authorId").value(author.getId()))
                .andExpect(jsonPath("$.content").value("First comment"))
                .andExpect(jsonPath("$.mentionedUsers.length()").value(0));
    }

    @Test
    void addCommentWithMissingTicketFails() throws Exception {
        User author = createUser("author", "author@example.com", "secret");
        String token = login("author", "secret");

        mockMvc.perform(post("/tickets/{ticketId}/comments", 9999)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "authorId": %d,
                                  "content": "No ticket"
                                }
                                """.formatted(author.getId())))
                .andExpect(status().isNotFound());
    }

    @Test
    void addCommentWithMissingAuthorFails() throws Exception {
        User owner = createUser("owner", "owner@example.com", "secret");
        String token = login("owner", "secret");
        long projectId = createProject(token, owner.getId(), "Comment Project");
        long ticketId = createTicket(token, projectId, owner.getId(), "TODO");

        mockMvc.perform(post("/tickets/{ticketId}/comments", ticketId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "authorId": 9999,
                                  "content": "No author"
                                }
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void getCommentsForTicketIncludesMentionedUsers() throws Exception {
        User author = createUser("author", "author@example.com", "secret");
        User mentioned = createUser("jdoe", "jdoe@example.com", "secret");
        String token = login("author", "secret");
        long projectId = createProject(token, author.getId(), "Mention Project");
        long ticketId = createTicket(token, projectId, author.getId(), "TODO");
        createComment(token, ticketId, author.getId(), "Hello @jdoe");

        mockMvc.perform(get("/tickets/{ticketId}/comments", ticketId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].mentionedUsers.length()").value(1))
                .andExpect(jsonPath("$[0].mentionedUsers[0].id").value(mentioned.getId()))
                .andExpect(jsonPath("$[0].mentionedUsers[0].username").value("jdoe"));
    }

    @Test
    void mentionParsingIsCaseInsensitive() throws Exception {
        User author = createUser("author", "author@example.com", "secret");
        User mentioned = createUser("CaseUser", "case@example.com", "secret");
        String token = login("author", "secret");
        long projectId = createProject(token, author.getId(), "Case Mention Project");
        long ticketId = createTicket(token, projectId, author.getId(), "TODO");

        mockMvc.perform(post("/tickets/{ticketId}/comments", ticketId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "authorId": %d,
                                  "content": "Please check this @caseuser"
                                }
                                """.formatted(author.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mentionedUsers.length()").value(1))
                .andExpect(jsonPath("$.mentionedUsers[0].id").value(mentioned.getId()))
                .andExpect(jsonPath("$.mentionedUsers[0].username").value("CaseUser"));
    }

    @Test
    void duplicateUsernameInSameCommentCreatesOneMention() throws Exception {
        User author = createUser("author", "author@example.com", "secret");
        createUser("dupe", "dupe@example.com", "secret");
        String token = login("author", "secret");
        long projectId = createProject(token, author.getId(), "Duplicate Mention Project");
        long ticketId = createTicket(token, projectId, author.getId(), "TODO");
        long commentId = createComment(token, ticketId, author.getId(), "Ping @dupe and @DUPE again");

        assertThat(mentionRepository.findByCommentId(commentId)).hasSize(1);
    }

    @Test
    void updatingCommentReevaluatesMentionsAndRemovesOldMentions() throws Exception {
        User author = createUser("author", "author@example.com", "secret");
        User oldMention = createUser("olduser", "old@example.com", "secret");
        User newMention = createUser("newuser", "new@example.com", "secret");
        String token = login("author", "secret");
        long projectId = createProject(token, author.getId(), "Update Mention Project");
        long ticketId = createTicket(token, projectId, author.getId(), "TODO");
        long commentId = createComment(token, ticketId, author.getId(), "Initial @olduser");

        mockMvc.perform(patch("/tickets/{ticketId}/comments/{commentId}", ticketId, commentId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "Updated @newuser"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/users/{userId}/mentions", oldMention.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0));

        mockMvc.perform(get("/users/{userId}/mentions", newMention.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.data[0].id").value(commentId))
                .andExpect(jsonPath("$.data[0].mentionedUsers[0].username").value("newuser"));
    }

    @Test
    void deletingCommentRemovesMentionAssociationsCleanly() throws Exception {
        User author = createUser("author", "author@example.com", "secret");
        User mentioned = createUser("mentioned", "mentioned@example.com", "secret");
        String token = login("author", "secret");
        long projectId = createProject(token, author.getId(), "Delete Mention Project");
        long ticketId = createTicket(token, projectId, author.getId(), "TODO");
        long commentId = createComment(token, ticketId, author.getId(), "Remove @mentioned");

        mockMvc.perform(delete("/tickets/{ticketId}/comments/{commentId}", ticketId, commentId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        assertThat(mentionRepository.findByCommentId(commentId)).isEmpty();
        assertThat(commentRepository.findById(commentId)).isEmpty();

        mockMvc.perform(get("/users/{userId}/mentions", mentioned.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void getUserMentionsReturnsNewestFirstAndPaginatedResponse() throws Exception {
        User author = createUser("author", "author@example.com", "secret");
        User target = createUser("target", "target@example.com", "secret");
        String token = login("author", "secret");
        long projectId = createProject(token, author.getId(), "Paged Mentions Project");
        long ticketId = createTicket(token, projectId, author.getId(), "TODO");
        long firstCommentId = createComment(token, ticketId, author.getId(), "First @target");
        long secondCommentId = createComment(token, ticketId, author.getId(), "Second @target");

        mockMvc.perform(get("/users/{userId}/mentions", target.getId())
                        .param("page", "0")
                        .param("pageSize", "1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(secondCommentId))
                .andExpect(jsonPath("$.data[0].content").value("Second @target"));

        mockMvc.perform(get("/users/{userId}/mentions", target.getId())
                        .param("page", "1")
                        .param("pageSize", "1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(firstCommentId))
                .andExpect(jsonPath("$.data[0].content").value("First @target"));
    }

    @Test
    void commentStateChangingActionsWriteAuditLog() throws Exception {
        User author = createUser("author", "author@example.com", "secret");
        String token = login("author", "secret");
        long projectId = createProject(token, author.getId(), "Audited Comments Project");
        long ticketId = createTicket(token, projectId, author.getId(), "TODO");
        long commentId = createComment(token, ticketId, author.getId(), "Audited comment");

        mockMvc.perform(patch("/tickets/{ticketId}/comments/{commentId}", ticketId, commentId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "Audited comment updated"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/tickets/{ticketId}/comments/{commentId}", ticketId, commentId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        List<AuditAction> actions = auditLogRepository
                .findByEntityTypeAndEntityIdOrderByTimestampDesc(AuditEntityType.COMMENT, commentId)
                .stream()
                .map(log -> log.getAction())
                .toList();

        assertThat(actions).contains(
                AuditAction.ADD_COMMENT,
                AuditAction.UPDATE_COMMENT,
                AuditAction.DELETE_COMMENT
        );
    }

    @Test
    void addDependencyBetweenTicketsInSameProjectSucceeds() throws Exception {
        User owner = createUser("owner", "owner@example.com", "secret");
        String token = login("owner", "secret");
        long projectId = createProject(token, owner.getId(), "Dependency Project");
        long ticketId = createTicket(token, projectId, owner.getId(), "TODO");
        long blockerId = createTicket(token, projectId, owner.getId(), "IN_PROGRESS");

        mockMvc.perform(post("/tickets/{ticketId}/dependencies", ticketId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "blockedBy": %d
                                }
                                """.formatted(blockerId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(blockerId))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void listDependenciesReturnsBlocker() throws Exception {
        User owner = createUser("owner", "owner@example.com", "secret");
        String token = login("owner", "secret");
        long projectId = createProject(token, owner.getId(), "List Dependencies Project");
        long ticketId = createTicket(token, projectId, owner.getId(), "TODO");
        long blockerId = createTicket(token, projectId, owner.getId(), "TODO");
        addDependency(token, ticketId, blockerId);

        mockMvc.perform(get("/tickets/{ticketId}/dependencies", ticketId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(blockerId))
                .andExpect(jsonPath("$[0].title").value("Test ticket"))
                .andExpect(jsonPath("$[0].status").value("TODO"));
    }

    @Test
    void removeDependencySucceeds() throws Exception {
        User owner = createUser("owner", "owner@example.com", "secret");
        String token = login("owner", "secret");
        long projectId = createProject(token, owner.getId(), "Remove Dependency Project");
        long ticketId = createTicket(token, projectId, owner.getId(), "TODO");
        long blockerId = createTicket(token, projectId, owner.getId(), "TODO");
        addDependency(token, ticketId, blockerId);

        mockMvc.perform(delete("/tickets/{ticketId}/dependencies/{blockerId}", ticketId, blockerId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        assertThat(ticketDependencyRepository.findByTicketIdAndBlockerTicketId(ticketId, blockerId)).isEmpty();
    }

    @Test
    void selfDependencyFails() throws Exception {
        User owner = createUser("owner", "owner@example.com", "secret");
        String token = login("owner", "secret");
        long projectId = createProject(token, owner.getId(), "Self Dependency Project");
        long ticketId = createTicket(token, projectId, owner.getId(), "TODO");

        mockMvc.perform(post("/tickets/{ticketId}/dependencies", ticketId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "blockedBy": %d
                                }
                                """.formatted(ticketId)))
                .andExpect(status().isConflict());
    }

    @Test
    void duplicateDependencyFails() throws Exception {
        User owner = createUser("owner", "owner@example.com", "secret");
        String token = login("owner", "secret");
        long projectId = createProject(token, owner.getId(), "Duplicate Dependency Project");
        long ticketId = createTicket(token, projectId, owner.getId(), "TODO");
        long blockerId = createTicket(token, projectId, owner.getId(), "TODO");
        addDependency(token, ticketId, blockerId);

        mockMvc.perform(post("/tickets/{ticketId}/dependencies", ticketId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "blockedBy": %d
                                }
                                """.formatted(blockerId)))
                .andExpect(status().isConflict());
    }

    @Test
    void crossProjectDependencyFails() throws Exception {
        User owner = createUser("owner", "owner@example.com", "secret");
        String token = login("owner", "secret");
        long firstProjectId = createProject(token, owner.getId(), "First Dependency Project");
        long secondProjectId = createProject(token, owner.getId(), "Second Dependency Project");
        long ticketId = createTicket(token, firstProjectId, owner.getId(), "TODO");
        long blockerId = createTicket(token, secondProjectId, owner.getId(), "TODO");

        mockMvc.perform(post("/tickets/{ticketId}/dependencies", ticketId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "blockedBy": %d
                                }
                                """.formatted(blockerId)))
                .andExpect(status().isConflict());
    }

    @Test
    void missingBlockerFails() throws Exception {
        User owner = createUser("owner", "owner@example.com", "secret");
        String token = login("owner", "secret");
        long projectId = createProject(token, owner.getId(), "Missing Blocker Project");
        long ticketId = createTicket(token, projectId, owner.getId(), "TODO");

        mockMvc.perform(post("/tickets/{ticketId}/dependencies", ticketId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "blockedBy": 9999
                                }
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void ticketCannotTransitionToDoneWhileBlockerIsUnresolved() throws Exception {
        User owner = createUser("owner", "owner@example.com", "secret");
        String token = login("owner", "secret");
        long projectId = createProject(token, owner.getId(), "Blocked Done Project");
        long ticketId = createTicket(token, projectId, owner.getId(), "IN_REVIEW");
        long blockerId = createTicket(token, projectId, owner.getId(), "TODO");
        addDependency(token, ticketId, blockerId);

        mockMvc.perform(patch("/tickets/{ticketId}", ticketId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "DONE"
                                }
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void ticketCanTransitionToDoneAfterBlockerIsDone() throws Exception {
        User owner = createUser("owner", "owner@example.com", "secret");
        String token = login("owner", "secret");
        long projectId = createProject(token, owner.getId(), "Unblocked Done Project");
        long ticketId = createTicket(token, projectId, owner.getId(), "IN_REVIEW");
        long blockerId = createTicket(token, projectId, owner.getId(), "DONE");
        addDependency(token, ticketId, blockerId);

        mockMvc.perform(patch("/tickets/{ticketId}", ticketId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "DONE"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/tickets/{ticketId}", ticketId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"));
    }

    @Test
    void addAndRemoveDependencyActionsWriteAuditLog() throws Exception {
        User owner = createUser("owner", "owner@example.com", "secret");
        String token = login("owner", "secret");
        long projectId = createProject(token, owner.getId(), "Audited Dependency Project");
        long ticketId = createTicket(token, projectId, owner.getId(), "TODO");
        long blockerId = createTicket(token, projectId, owner.getId(), "TODO");

        long dependencyId = addDependency(token, ticketId, blockerId);
        mockMvc.perform(delete("/tickets/{ticketId}/dependencies/{blockerId}", ticketId, blockerId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        List<AuditAction> actions = auditLogRepository
                .findByEntityTypeAndEntityIdOrderByTimestampDesc(AuditEntityType.TICKET_DEPENDENCY, dependencyId)
                .stream()
                .map(log -> log.getAction())
                .toList();

        assertThat(actions).contains(
                AuditAction.ADD_DEPENDENCY,
                AuditAction.REMOVE_DEPENDENCY
        );
    }

    @Test
    void creatingTicketWithExplicitAssigneeKeepsThatAssignee() throws Exception {
        User admin = createUser("admin", "admin@example.com", "secret", "ADMIN");
        User firstDeveloper = createUser("firstdev", "firstdev@example.com", "secret");
        User explicitAssignee = createUser("explicitdev", "explicitdev@example.com", "secret");
        String token = login("admin", "secret");
        long projectId = createProject(token, admin.getId(), "Explicit Assignment Project");

        createTicket(token, projectId, firstDeveloper.getId(), "TODO");
        long ticketId = createTicket(token, projectId, explicitAssignee.getId(), "TODO");

        mockMvc.perform(get("/tickets/{ticketId}", ticketId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assigneeId").value(explicitAssignee.getId()));
    }

    @Test
    void creatingTicketWithoutAssigneeAutoAssignsLeastLoadedDeveloper() throws Exception {
        User admin = createUser("admin", "admin@example.com", "secret", "ADMIN");
        User busyDeveloper = createUser("busydev", "busydev@example.com", "secret");
        User leastLoadedDeveloper = createUser("leastdev", "leastdev@example.com", "secret");
        String token = login("admin", "secret");
        long projectId = createProject(token, admin.getId(), "Least Loaded Project");
        createTicket(token, projectId, busyDeveloper.getId(), "TODO");

        long ticketId = createTicket(token, projectId, null, "TODO");

        mockMvc.perform(get("/tickets/{ticketId}", ticketId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assigneeId").value(leastLoadedDeveloper.getId()));
    }

    @Test
    void adminUsersAreExcludedFromAutoAssignment() throws Exception {
        User admin = createUser("admin", "admin@example.com", "secret", "ADMIN");
        User developer = createUser("dev", "dev@example.com", "secret");
        String token = login("admin", "secret");
        long projectId = createProject(token, admin.getId(), "Admin Excluded Project");
        createTicket(token, projectId, developer.getId(), "TODO");

        long ticketId = createTicket(token, projectId, null, "TODO");

        mockMvc.perform(get("/tickets/{ticketId}", ticketId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assigneeId").value(developer.getId()));
    }

    @Test
    void autoAssignmentTiesAreBrokenByLowestUserId() throws Exception {
        User admin = createUser("admin", "admin@example.com", "secret", "ADMIN");
        User oldestDeveloper = createUser("oldestdev", "oldestdev@example.com", "secret");
        createUser("newestdev", "newestdev@example.com", "secret");
        String token = login("admin", "secret");
        long projectId = createProject(token, admin.getId(), "Tie Break Project");

        long ticketId = createTicket(token, projectId, null, "TODO");

        mockMvc.perform(get("/tickets/{ticketId}", ticketId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assigneeId").value(oldestDeveloper.getId()));
    }

    @Test
    void ifNoDeveloperExistsTicketIsCreatedUnassigned() throws Exception {
        User admin = createUser("admin", "admin@example.com", "secret", "ADMIN");
        String token = login("admin", "secret");
        long projectId = createProject(token, admin.getId(), "No Developers Project");

        long ticketId = createTicket(token, projectId, null, "TODO");

        mockMvc.perform(get("/tickets/{ticketId}", ticketId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assigneeId").doesNotExist());
    }

    @Test
    void autoAssignmentIsNotTriggeredOnPatch() throws Exception {
        User admin = createUser("admin", "admin@example.com", "secret", "ADMIN");
        User assignedDeveloper = createUser("assigneddev", "assigneddev@example.com", "secret");
        createUser("otherdev", "otherdev@example.com", "secret");
        String token = login("admin", "secret");
        long projectId = createProject(token, admin.getId(), "Patch Assignment Project");
        long ticketId = createTicket(token, projectId, assignedDeveloper.getId(), "TODO");

        mockMvc.perform(patch("/tickets/{ticketId}", ticketId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Updated without assignee"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/tickets/{ticketId}", ticketId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assigneeId").value(assignedDeveloper.getId()));
    }

    @Test
    void workloadEndpointReturnsOpenTicketCountsPerDeveloperInProject() throws Exception {
        User admin = createUser("admin", "admin@example.com", "secret", "ADMIN");
        User firstDeveloper = createUser("firstdev", "firstdev@example.com", "secret");
        User secondDeveloper = createUser("seconddev", "seconddev@example.com", "secret");
        String token = login("admin", "secret");
        long projectId = createProject(token, admin.getId(), "Workload Project");
        long otherProjectId = createProject(token, admin.getId(), "Other Workload Project");
        createTicket(token, projectId, firstDeveloper.getId(), "TODO");
        createTicket(token, projectId, firstDeveloper.getId(), "IN_PROGRESS");
        createTicket(token, otherProjectId, firstDeveloper.getId(), "TODO");

        mockMvc.perform(get("/projects/{projectId}/workload", projectId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].userId").value(secondDeveloper.getId()))
                .andExpect(jsonPath("$[0].openTicketCount").value(0))
                .andExpect(jsonPath("$[1].userId").value(firstDeveloper.getId()))
                .andExpect(jsonPath("$[1].openTicketCount").value(2));
    }

    @Test
    void doneTicketsAreExcludedFromWorkloadCount() throws Exception {
        User admin = createUser("admin", "admin@example.com", "secret", "ADMIN");
        User developer = createUser("dev", "dev@example.com", "secret");
        String token = login("admin", "secret");
        long projectId = createProject(token, admin.getId(), "Done Excluded Workload Project");
        createTicket(token, projectId, developer.getId(), "TODO");
        createTicket(token, projectId, developer.getId(), "DONE");

        mockMvc.perform(get("/projects/{projectId}/workload", projectId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].userId").value(developer.getId()))
                .andExpect(jsonPath("$[0].openTicketCount").value(1));
    }

    @Test
    void deletedTicketsAreExcludedFromWorkloadCount() throws Exception {
        User admin = createUser("admin", "admin@example.com", "secret", "ADMIN");
        User developer = createUser("dev", "dev@example.com", "secret");
        String token = login("admin", "secret");
        long projectId = createProject(token, admin.getId(), "Deleted Excluded Workload Project");
        long deletedTicketId = createTicket(token, projectId, developer.getId(), "TODO");

        mockMvc.perform(delete("/tickets/{ticketId}", deletedTicketId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/projects/{projectId}/workload", projectId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].userId").value(developer.getId()))
                .andExpect(jsonPath("$[0].openTicketCount").value(0));
    }

    @Test
    void autoAssignmentRecordsSystemAuditLog() throws Exception {
        User admin = createUser("admin", "admin@example.com", "secret", "ADMIN");
        User developer = createUser("dev", "dev@example.com", "secret");
        String token = login("admin", "secret");
        long projectId = createProject(token, admin.getId(), "Auto Assign Audit Project");

        long ticketId = createTicket(token, projectId, null, "TODO");

        assertThat(auditLogRepository
                .findByEntityTypeAndEntityIdOrderByTimestampDesc(AuditEntityType.TICKET, ticketId))
                .anySatisfy(log -> {
                    assertThat(log.getAction()).isEqualTo(AuditAction.AUTO_ASSIGN);
                    assertThat(log.getActor()).isEqualTo(ActorType.SYSTEM);
                    assertThat(log.getPerformedBy()).isNull();
                });

        mockMvc.perform(get("/tickets/{ticketId}", ticketId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assigneeId").value(developer.getId()));
    }

    private User createUser(String username, String email, String password) throws Exception {
        return createUser(username, email, password, "DEVELOPER");
    }

    private User createUser(String username, String email, String password, String role) throws Exception {
        String passwordJson = password == null ? "" : ", \"password\": \"" + password + "\"";
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "email": "%s",
                                  "fullName": "Test User",
                                  "role": "%s"%s
                                }
                                """.formatted(username, email, role, passwordJson)))
                .andExpect(status().isOk());
        return userRepository.findByUsernameIgnoreCase(username).orElseThrow();
    }

    private String login(String username, String password) throws Exception {
        String response = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "%s"
                                }
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(response);
        return json.get("accessToken").asText();
    }

    private long createProject(String token, Long ownerId, String name) throws Exception {
        String response = mockMvc.perform(post("/projects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "description": "A sample project",
                                  "ownerId": %d
                                }
                                """.formatted(name, ownerId)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).get("id").asLong();
    }

    private long createTicket(String token, Long projectId, Long assigneeId, String ticketStatus) throws Exception {
        String assigneeJson = assigneeId == null ? "" : ", \"assigneeId\": " + assigneeId;
        String response = mockMvc.perform(post("/tickets")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Test ticket",
                                  "description": "A test ticket",
                                  "status": "%s",
                                  "priority": "HIGH",
                                  "type": "BUG",
                                  "projectId": %d%s
                                }
                                """.formatted(ticketStatus, projectId, assigneeJson)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).get("id").asLong();
    }

    private long createComment(String token, Long ticketId, Long authorId, String content) throws Exception {
        String response = mockMvc.perform(post("/tickets/{ticketId}/comments", ticketId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "authorId": %d,
                                  "content": "%s"
                                }
                                """.formatted(authorId, content)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).get("id").asLong();
    }

    private long addDependency(String token, Long ticketId, Long blockerId) throws Exception {
        mockMvc.perform(post("/tickets/{ticketId}/dependencies", ticketId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "blockedBy": %d
                                }
                                """.formatted(blockerId)))
                .andExpect(status().isOk());

        return ticketDependencyRepository.findByTicketIdAndBlockerTicketId(ticketId, blockerId)
                .orElseThrow()
                .getId();
    }
}
