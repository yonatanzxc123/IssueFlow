package com.att.tdp.issueflow.repository;

import com.att.tdp.issueflow.domain.Project;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findByDeletedFalseOrderByIdAsc();

    List<Project> findByDeletedTrueOrderByDeletedAtDesc();

    Optional<Project> findByIdAndDeletedFalse(Long id);
}
