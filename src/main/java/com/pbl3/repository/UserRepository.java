package com.pbl3.repository;

import com.pbl3.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;


public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsBypassword(String password);
    Optional<User> findByUsername(String username);
}
