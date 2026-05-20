package com.att.tdp.issueflow.web.mapper;

import com.att.tdp.issueflow.domain.Comment;
import com.att.tdp.issueflow.domain.User;
import com.att.tdp.issueflow.web.dto.response.CommentResponse;
import com.att.tdp.issueflow.web.dto.response.MentionedCommentResponse;
import com.att.tdp.issueflow.web.dto.response.MentionedUserResponse;
import java.util.List;

public final class CommentMapper {

    private CommentMapper() {
    }

    public static CommentResponse toResponse(Comment comment, List<User> mentionedUsers) {
        return new CommentResponse(
                comment.getId(),
                comment.getTicket().getId(),
                comment.getAuthor().getId(),
                comment.getContent(),
                toMentionedUsers(mentionedUsers)
        );
    }

    public static CommentResponse toResponse(Comment comment) {
        return toResponse(comment, List.of());
    }

    public static MentionedCommentResponse toMentionedCommentResponse(Comment comment, List<User> mentionedUsers) {
        return new MentionedCommentResponse(
                comment.getId(),
                comment.getTicket().getId(),
                comment.getAuthor().getId(),
                comment.getContent(),
                toMentionedUsers(mentionedUsers)
        );
    }

    private static List<MentionedUserResponse> toMentionedUsers(List<User> mentionedUsers) {
        return mentionedUsers.stream()
                .map(UserMapper::toMentionedUserResponse)
                .toList();
    }
}
