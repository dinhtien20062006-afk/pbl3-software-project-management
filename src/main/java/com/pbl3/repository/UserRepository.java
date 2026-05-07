package com.pbl3.repository;

import com.pbl3.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByUsername(String username);
    Optional<User> findByUsername(String username);
    List<User> findAllByRole(User.Role role);
    List<User> findByUsernameContainingIgnoreCase(String keyword);

    // Đếm theo Role cụ thể
    long countByRole(User.Role role);

}