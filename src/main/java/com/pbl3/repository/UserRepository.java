package com.pbl3.repository;

import com.pbl3.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByUsername(String username);
    Optional<User> findByUsername(String username);
    List<User> findAllByRole(User.Role role);
    List<User> findByUsernameContainingIgnoreCase(String keyword);

    // Đếm tổng số lượng User (Dành cho thẻ Stat của Admin)
    @Query("SELECT COUNT(u) FROM User u")
    long countTotalUsers();
}