package com.att.tdp.issueflow.web.controller;

import com.att.tdp.issueflow.service.ProjectService;
import com.att.tdp.issueflow.web.dto.request.CreateProjectRequest;
import com.att.tdp.issueflow.web.dto.request.UpdateProjectRequest;
import com.att.tdp.issueflow.web.dto.response.ProjectResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping
    public List<ProjectResponse> getAllProjects() {
        return projectService.getAllProjects();
    }

    @GetMapping("/{projectId}")
    public ProjectResponse getProject(@PathVariable Long projectId) {
        return projectService.getProject(projectId);
    }

    @PostMapping
    public ProjectResponse createProject(@Valid @RequestBody CreateProjectRequest request) {
        return projectService.createProject(request);
    }

    @PatchMapping("/{projectId}")
    public ProjectResponse updateProject(
            @PathVariable Long projectId,
            @Valid @RequestBody UpdateProjectRequest request
    ) {
        return projectService.updateProject(projectId, request);
    }

    @DeleteMapping("/{projectId}")
    public ResponseEntity<Void> deleteProject(@PathVariable Long projectId) {
        projectService.deleteProject(projectId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/deleted")
    @PreAuthorize("hasRole('ADMIN')")
    public List<ProjectResponse> getDeletedProjects() {
        return projectService.getDeletedProjects();
    }

    @PostMapping("/{projectId}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    public ProjectResponse restoreProject(@PathVariable Long projectId) {
        return projectService.restoreProject(projectId);
    }
}
