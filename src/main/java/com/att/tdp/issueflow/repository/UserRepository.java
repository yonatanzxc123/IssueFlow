package com.att.tdp.issueflow.repository;

import com.att.tdp.issueflow.domain.User;
import com.att.tdp.issueflow.domain.enums.Role;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByEmailIgnoreCase(String email);

    List<User> findByRoleOrderByCreatedAtAsc(Role role);
}
