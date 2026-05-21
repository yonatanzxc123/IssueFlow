package com.att.tdp.issueflow.repository;

import com.att.tdp.issueflow.domain.Mention;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MentionRepository extends JpaRepository<Mention, Long> {

    List<Mention> findByCommentId(Long commentId);

    @Query("""
            select mention
            from Mention mention
            where mention.mentionedUser.id = :mentionedUserId
            order by mention.comment.createdAt desc, mention.comment.id desc
            """)
    Page<Mention> findMentionedComments(@Param("mentionedUserId") Long mentionedUserId, Pageable pageable);

    void deleteByCommentId(Long commentId);
}
