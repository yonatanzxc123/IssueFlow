package com.att.tdp.issueflow.web.mapper;

import com.att.tdp.issueflow.domain.Project;
import com.att.tdp.issueflow.web.dto.response.ProjectResponse;

public final class ProjectMapper {

    private ProjectMapper() {
    }

    public static ProjectResponse toResponse(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getOwner().getId()
        );
    }
}
