package com.att.tdp.issueflow.repository;

import com.att.tdp.issueflow.domain.Comment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByTicketIdOrderByCreatedAtAsc(Long ticketId);

    Optional<Comment> findByIdAndTicketId(Long id, Long ticketId);
}
