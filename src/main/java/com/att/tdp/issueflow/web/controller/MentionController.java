package com.att.tdp.issueflow.web.controller;

import com.att.tdp.issueflow.service.CommentService;
import com.att.tdp.issueflow.web.dto.response.MentionsPageResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users/{userId}/mentions")
public class MentionController {

    private final CommentService commentService;

    public MentionController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping
    public MentionsPageResponse getMentions(
            @PathVariable Long userId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize
    ) {
        return commentService.getMentionsForUser(userId, page, pageSize);
    }
}
