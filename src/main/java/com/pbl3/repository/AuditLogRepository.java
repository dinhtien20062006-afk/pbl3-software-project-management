package com.pbl3.repository;

import com.pbl3.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    // 1. Lấy log của 1 project cụ thể (Dùng JOIN FETCH để tránh N+1 query cho User)
    @Query("SELECT a FROM AuditLog a " +
           "JOIN FETCH a.user " +
           "WHERE a.project.id = :projectId " +
           "ORDER BY a.createdAt DESC")
    List<AuditLog> findAllByProjectId(@Param("projectId") Long projectId);

    // 2. Lấy log toàn hệ thống (Dành cho ADMIN)
    @Query("SELECT a FROM AuditLog a " +
           "JOIN FETCH a.user " +
           "ORDER BY a.createdAt DESC")
    List<AuditLog> findAllLogs();

    // Lấy hoạt động mới nhất cho Dashboard (Giới hạn số lượng bằng Pageable)
    // Admin: Xem toàn bộ
    @Query("SELECT a FROM AuditLog a ORDER BY a.createdAt DESC")
    List<AuditLog> findRecentLogs(Pageable pageable);

    // Manager/Member: Chỉ xem log thuộc dự án mình tham gia
    @Query("SELECT a FROM AuditLog a JOIN ProjectMember pm ON a.project.id = pm.project.id " +
           "WHERE pm.user.id = :userId AND pm.leftAt IS NULL " +
           "ORDER BY a.createdAt DESC")
    List<AuditLog> findRecentLogsByUserProjects(@Param("userId") Long userId, Pageable pageable);
}