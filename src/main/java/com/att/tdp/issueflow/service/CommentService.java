package com.att.tdp.issueflow.service;

import com.att.tdp.issueflow.domain.Comment;
import com.att.tdp.issueflow.domain.Mention;
import com.att.tdp.issueflow.domain.Ticket;
import com.att.tdp.issueflow.domain.User;
import com.att.tdp.issueflow.domain.enums.AuditAction;
import com.att.tdp.issueflow.exception.ResourceNotFoundException;
import com.att.tdp.issueflow.repository.CommentRepository;
import com.att.tdp.issueflow.repository.MentionRepository;
import com.att.tdp.issueflow.repository.TicketRepository;
import com.att.tdp.issueflow.repository.UserRepository;
import com.att.tdp.issueflow.web.dto.request.CreateCommentRequest;
import com.att.tdp.issueflow.web.dto.request.UpdateCommentRequest;
import com.att.tdp.issueflow.web.dto.response.CommentResponse;
import com.att.tdp.issueflow.web.dto.response.MentionedCommentResponse;
import com.att.tdp.issueflow.web.dto.response.MentionsPageResponse;
import com.att.tdp.issueflow.web.mapper.CommentMapper;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommentService {

    private static final Pattern MENTION_PATTERN = Pattern.compile("(?<![A-Za-z0-9_])@([A-Za-z0-9._-]+)");
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_PAGE_SIZE = 20;

    private final CommentRepository commentRepository;
    private final MentionRepository mentionRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final AuditLogService auditLogService;

    public CommentService(
            CommentRepository commentRepository,
            MentionRepository mentionRepository,
            TicketRepository ticketRepository,
            UserRepository userRepository,
            UserService userService,
            AuditLogService auditLogService
    ) {
        this.commentRepository = commentRepository;
        this.mentionRepository = mentionRepository;
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.userService = userService;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> getComments(Long ticketId) {
        findActiveTicket(ticketId);
        return commentRepository.findByTicketIdOrderByCreatedAtAsc(ticketId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public CommentResponse addComment(Long ticketId, CreateCommentRequest request) {
        Ticket ticket = findActiveTicket(ticketId);
        User author = userService.findUser(request.authorId());

        Comment comment = new Comment();
        comment.setTicket(ticket);
        comment.setAuthor(author);
        comment.setContent(request.content());

        Comment saved = commentRepository.save(comment);
        saveMentions(saved, request.content());
        auditLogService.recordCommentAction(AuditAction.ADD_COMMENT, saved.getId());
        return toResponse(saved);
    }

    @Transactional
    public void updateComment(Long ticketId, Long commentId, UpdateCommentRequest request) {
        findActiveTicket(ticketId);
        Comment comment = findCommentForTicket(ticketId, commentId);

        comment.setContent(request.content());
        mentionRepository.deleteByCommentId(comment.getId());
        saveMentions(comment, request.content());
        auditLogService.recordCommentAction(AuditAction.UPDATE_COMMENT, comment.getId());
    }

    @Transactional
    public void deleteComment(Long ticketId, Long commentId) {
        findActiveTicket(ticketId);
        Comment comment = findCommentForTicket(ticketId, commentId);

        mentionRepository.deleteByCommentId(comment.getId());
        commentRepository.delete(comment);
        auditLogService.recordCommentAction(AuditAction.DELETE_COMMENT, comment.getId());
    }

    @Transactional(readOnly = true)
    public MentionsPageResponse getMentionsForUser(Long userId, Integer page, Integer pageSize) {
        userService.findUser(userId);
        int resolvedPage = page == null ? DEFAULT_PAGE : Math.max(page, 0);
        int resolvedPageSize = pageSize == null ? DEFAULT_PAGE_SIZE : Math.max(pageSize, 1);

        Page<Mention> mentions = mentionRepository.findMentionedComments(
                userId,
                PageRequest.of(resolvedPage, resolvedPageSize)
        );

        List<MentionedCommentResponse> data = mentions.getContent()
                .stream()
                .map(Mention::getComment)
                .map(comment -> CommentMapper.toMentionedCommentResponse(comment, mentionedUsersFor(comment)))
                .toList();

        return new MentionsPageResponse(data, mentions.getTotalElements(), resolvedPage);
    }

    private Ticket findActiveTicket(Long ticketId) {
        return ticketRepository.findByIdAndDeletedFalse(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));
    }

    private Comment findCommentForTicket(Long ticketId, Long commentId) {
        return commentRepository.findByIdAndTicketId(commentId, ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));
    }

    private CommentResponse toResponse(Comment comment) {
        return CommentMapper.toResponse(comment, mentionedUsersFor(comment));
    }

    private List<User> mentionedUsersFor(Comment comment) {
        return mentionRepository.findByCommentId(comment.getId())
                .stream()
                .map(Mention::getMentionedUser)
                .sorted(Comparator.comparing(User::getUsername, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private void saveMentions(Comment comment, String content) {
        parseMentionedUsers(content).forEach(user -> {
            Mention mention = new Mention();
            mention.setComment(comment);
            mention.setMentionedUser(user);
            mentionRepository.save(mention);
        });
    }

    private List<User> parseMentionedUsers(String content) {
        Map<String, User> usersByLowercaseUsername = new LinkedHashMap<>();
        Matcher matcher = MENTION_PATTERN.matcher(content);
        while (matcher.find()) {
            String username = matcher.group(1);
            String key = username.toLowerCase(Locale.ROOT);
            if (!usersByLowercaseUsername.containsKey(key)) {
                userRepository.findByUsernameIgnoreCase(username)
                        .ifPresent(user -> usersByLowercaseUsername.put(key, user));
            }
        }
        return List.copyOf(usersByLowercaseUsername.values());
    }
}
