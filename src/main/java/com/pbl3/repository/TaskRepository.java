package com.pbl3.repository;

import com.pbl3.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByProjectId(Long projectId);
    List<Task> findByProjectIdAndAssigneeId(Long projectId, Long assigneeId);
    List<Task> findByProjectTeamId(Long teamId);
    long countByProjectIdAndStatus(Long projectId, String status);

    // Lọc task theo trạng thái, độ ưu tiên, người được giao trong nhóm
    @Query("SELECT t FROM Task t WHERE t.projectTeam.id = :teamId " +
           "AND (:status IS NULL OR t.status = :status) " +
           "AND (:priority IS NULL OR t.priority = :priority) " +
           "AND (:assigneeId IS NULL OR t.assignee.id = :assigneeId) " +
           "ORDER BY t.createdAt DESC")
    List<Task> filterTeamTasks(@Param("teamId") Long teamId,
                               @Param("status") Task.TaskStatus status,
                               @Param("priority") Task.TaskPriority priority,
                               @Param("assigneeId") Long assigneeId);

    // Lọc task sắp hết hạn trong nhóm (deadline trong vòng X ngày) - Dành cho Leader
    @Query("SELECT t FROM Task t WHERE t.projectTeam.id = :teamId " +
           "AND t.status != 'DONE' " +
           "AND t.deadline BETWEEN :now AND :limitDate " +
           "ORDER BY t.deadline ASC")
    List<Task> findUrgentTasksByTeam(@Param("teamId") Long teamId, 
                                     @Param("now") LocalDate now, 
                                     @Param("limitDate") LocalDate limitDate);
}