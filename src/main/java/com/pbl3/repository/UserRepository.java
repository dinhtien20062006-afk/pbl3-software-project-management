package com.pbl3.repository;

import com.pbl3.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository làm việc với database
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Tìm user theo email (phục vụ login)
     */
    Optional<User> findByEmail(String email);
}