package com.att.tdp.issueflow.web.mapper;

import com.att.tdp.issueflow.domain.User;
import com.att.tdp.issueflow.web.dto.response.MentionedUserResponse;
import com.att.tdp.issueflow.web.dto.response.UserResponse;

public final class UserMapper {

    private UserMapper() {
    }

    public static UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getRole()
        );
    }

    public static MentionedUserResponse toMentionedUserResponse(User user) {
        return new MentionedUserResponse(
                user.getId(),
                user.getUsername(),
                user.getFullName()
        );
    }
}
