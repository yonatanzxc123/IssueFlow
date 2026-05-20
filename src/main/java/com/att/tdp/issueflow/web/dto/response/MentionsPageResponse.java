package com.att.tdp.issueflow.web.dto.response;

import java.util.List;

public record MentionsPageResponse(
        List<MentionedCommentResponse> data,
        long total,
        int page
) {
}
