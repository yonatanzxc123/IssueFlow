package com.att.tdp.issueflow.repository;

import com.att.tdp.issueflow.domain.Mention;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MentionRepository extends JpaRepository<Mention, Long> {

    List<Mention> findByCommentId(Long commentId);

    Page<Mention> findByMentionedUserIdOrderByCommentCreatedAtDesc(Long mentionedUserId, Pageable pageable);

    void deleteByCommentId(Long commentId);
}
