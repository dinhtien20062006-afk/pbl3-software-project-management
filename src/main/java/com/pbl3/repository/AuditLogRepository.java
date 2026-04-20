package com.pbl3.repository;

import com.pbl3.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    // Lấy log theo entity (TASK, PROJECT...)
    List<AuditLog> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(
            String entityType,
            Long entityId
    );

    // Lấy log theo user
    List<AuditLog> findByUserIdOrderByCreatedAtDesc(Long userId);

    // Lấy tất cả log
    List<AuditLog> findAllByOrderByCreatedAtDesc();
}