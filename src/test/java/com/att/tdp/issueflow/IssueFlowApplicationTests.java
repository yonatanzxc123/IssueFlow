package com.att.tdp.issueflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.att.tdp.issueflow.domain.User;
import com.att.tdp.issueflow.domain.enums.AuditAction;
import com.att.tdp.issueflow.domain.enums.AuditEntityType;
import com.att.tdp.issueflow.repository.AuditLogRepository;
import com.att.tdp.issueflow.repository.ProjectRepository;
import com.att.tdp.issueflow.repository.RevokedTokenRepository;
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
    private final AuditLogRepository auditLogRepository;
    private final RevokedTokenRepository revokedTokenRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    IssueFlowApplicationTests(
            MockMvc mockMvc,
            ObjectMapper objectMapper,
            UserRepository userRepository,
            ProjectRepository projectRepository,
            AuditLogRepository auditLogRepository,
            RevokedTokenRepository revokedTokenRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.auditLogRepository = auditLogRepository;
        this.revokedTokenRepository = revokedTokenRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @BeforeEach
    void cleanDatabase() {
        auditLogRepository.deleteAll();
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
}
