package com.att.tdp.issueflow.service;

import com.att.tdp.issueflow.domain.Project;
import com.att.tdp.issueflow.domain.User;
import com.att.tdp.issueflow.domain.enums.AuditAction;
import com.att.tdp.issueflow.domain.enums.Role;
import com.att.tdp.issueflow.domain.enums.TicketStatus;
import com.att.tdp.issueflow.exception.ResourceNotFoundException;
import com.att.tdp.issueflow.repository.ProjectRepository;
import com.att.tdp.issueflow.repository.TicketRepository;
import com.att.tdp.issueflow.repository.UserRepository;
import com.att.tdp.issueflow.web.dto.request.CreateProjectRequest;
import com.att.tdp.issueflow.web.dto.request.UpdateProjectRequest;
import com.att.tdp.issueflow.web.dto.response.ProjectResponse;
import com.att.tdp.issueflow.web.dto.response.WorkloadResponse;
import com.att.tdp.issueflow.web.mapper.ProjectMapper;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final AuditLogService auditLogService;

    public ProjectService(
            ProjectRepository projectRepository,
            TicketRepository ticketRepository,
            UserRepository userRepository,
            UserService userService,
            AuditLogService auditLogService
    ) {
        this.projectRepository = projectRepository;
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.userService = userService;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> getAllProjects() {
        return projectRepository.findByDeletedFalseOrderByIdAsc()
                .stream()
                .map(ProjectMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProject(Long projectId) {
        return ProjectMapper.toResponse(findActiveProject(projectId));
    }

    @Transactional(readOnly = true)
    public List<WorkloadResponse> getProjectWorkload(Long projectId) {
        findActiveProject(projectId);
        return userRepository.findByRoleOrderByIdAsc(Role.DEVELOPER)
                .stream()
                .map(user -> new WorkloadResponse(
                        user.getId(),
                        user.getUsername(),
                        countOpenTickets(projectId, user.getId())
                ))
                .sorted(Comparator
                        .comparingLong(WorkloadResponse::openTicketCount)
                        .thenComparing(WorkloadResponse::userId))
                .toList();
    }

    @Transactional
    public ProjectResponse createProject(CreateProjectRequest request) {
        User owner = userService.findUser(request.ownerId());

        Project project = new Project();
        project.setName(request.name());
        project.setDescription(request.description());
        project.setOwner(owner);

        Project saved = projectRepository.save(project);
        auditLogService.recordProjectAction(AuditAction.CREATE_PROJECT, saved.getId());
        return ProjectMapper.toResponse(saved);
    }

    @Transactional
    public ProjectResponse updateProject(Long projectId, UpdateProjectRequest request) {
        Project project = findActiveProject(projectId);
        if (request.name() != null) {
            project.setName(request.name());
        }
        if (request.description() != null) {
            project.setDescription(request.description());
        }
        auditLogService.recordProjectAction(AuditAction.UPDATE_PROJECT, project.getId());
        return ProjectMapper.toResponse(project);
    }

    @Transactional
    public void deleteProject(Long projectId) {
        Project project = findActiveProject(projectId);
        project.setDeleted(true);
        project.setDeletedAt(Instant.now());
        auditLogService.recordProjectAction(AuditAction.DELETE_PROJECT, project.getId());
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> getDeletedProjects() {
        return projectRepository.findByDeletedTrueOrderByDeletedAtDesc()
                .stream()
                .map(ProjectMapper::toResponse)
                .toList();
    }

    @Transactional
    public ProjectResponse restoreProject(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
        if (project.isDeleted()) {
            project.setDeleted(false);
            project.setDeletedAt(null);
            auditLogService.recordProjectAction(AuditAction.RESTORE_PROJECT, project.getId());
        }
        return ProjectMapper.toResponse(project);
    }

    private Project findActiveProject(Long projectId) {
        return projectRepository.findByIdAndDeletedFalse(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
    }

    private long countOpenTickets(Long projectId, Long userId) {
        return ticketRepository.countByProjectIdAndAssigneeIdAndStatusNotAndDeletedFalse(
                projectId,
                userId,
                TicketStatus.DONE
        );
    }
}
