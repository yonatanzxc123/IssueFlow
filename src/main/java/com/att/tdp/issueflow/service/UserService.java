package com.att.tdp.issueflow.service;

import com.att.tdp.issueflow.domain.User;
import com.att.tdp.issueflow.domain.enums.AuditAction;
import com.att.tdp.issueflow.exception.ConflictException;
import com.att.tdp.issueflow.exception.ResourceNotFoundException;
import com.att.tdp.issueflow.repository.UserRepository;
import com.att.tdp.issueflow.web.dto.request.CreateUserRequest;
import com.att.tdp.issueflow.web.dto.request.UpdateUserRequest;
import com.att.tdp.issueflow.web.dto.response.UserResponse;
import com.att.tdp.issueflow.web.mapper.UserMapper;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private static final String DEFAULT_DEVELOPMENT_PASSWORD = "secret";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(UserMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserResponse getUser(Long userId) {
        return UserMapper.toResponse(findUser(userId));
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        ensureUsernameAvailable(request.username());
        ensureEmailAvailable(request.email());

        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setFullName(request.fullName());
        user.setRole(request.role());
        user.setPasswordHash(passwordEncoder.encode(resolvePassword(request.password())));

        User saved = userRepository.save(user);
        auditLogService.recordPublicUserCreation(saved.getId());
        return UserMapper.toResponse(saved);
    }

    @Transactional
    public void updateUser(Long userId, UpdateUserRequest request) {
        User user = findUser(userId);
        if (request.fullName() != null) {
            user.setFullName(request.fullName());
        }
        if (request.role() != null) {
            user.setRole(request.role());
        }
        auditLogService.recordUserAction(AuditAction.UPDATE_USER, user.getId());
    }

    @Transactional
    public void deleteUser(Long userId) {
        User user = findUser(userId);
        userRepository.delete(user);
        auditLogService.recordUserDeletion(userId);
    }

    @Transactional(readOnly = true)
    public User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private void ensureUsernameAvailable(String username) {
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new ConflictException("Username already exists");
        }
    }

    private void ensureEmailAvailable(String email) {
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Email already exists");
        }
    }

    private String resolvePassword(String password) {
        if (password != null && !password.isBlank()) {
            return password;
        }
        // Default password exists only to preserve compatibility with the provided README API contract.
        return DEFAULT_DEVELOPMENT_PASSWORD;
    }
}
