package com.pbl3.repository;

import com.pbl3.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    // Lay log cua 1 project cu the. LEFT JOIN FETCH project de van giu duoc log co project = null neu can mo rong sau nay.
    @Query("SELECT a FROM AuditLog a " +
           "JOIN FETCH a.user " +
           "LEFT JOIN FETCH a.project " +
           "WHERE a.project.id = :projectId " +
           "ORDER BY a.createdAt DESC")
    List<AuditLog> findAllByProjectId(@Param("projectId") Long projectId);

    // Lay log toan he thong. LEFT JOIN FETCH de tranh N+1 query khi map response.
    @Query("SELECT a FROM AuditLog a " +
           "JOIN FETCH a.user " +
           "LEFT JOIN FETCH a.project " +
           "ORDER BY a.createdAt DESC")
    List<AuditLog> findAllLogs();

    @Modifying
    @Query("UPDATE AuditLog a SET a.project = null WHERE a.project.id = :projectId")
    void decoupleLogsFromProject(@Param("projectId") Long projectId);
}
