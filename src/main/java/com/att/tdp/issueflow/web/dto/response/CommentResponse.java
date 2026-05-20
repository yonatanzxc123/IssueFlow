package com.att.tdp.issueflow.web.dto.response;

import java.util.List;

public record CommentResponse(
        Long id,
        Long ticketId,
        Long authorId,
        String content,
        List<MentionedUserResponse> mentionedUsers
) {
}
