package com.pbl3.repository;

import com.pbl3.entity.Task;
import com.pbl3.entity.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;
import com.pbl3.entity.TaskPriority;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByProjectId(Long projectId);
    List<Task> findByStatus(TaskStatus status);
    List<Task> findByAssigneeId(Long userId);
    List<Task> findByProjectIdOrderByPriorityAsc(Long projectId);
    List<Task> findAllByOrderByPriorityDesc();
    List<Task> findByPriority(TaskPriority priority);
    List<Task> findAllByDeadlineBetween(LocalDateTime start, LocalDateTime end, TaskStatus status);
    long countByProjectIdAndStatus(Long projectId, String status);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.project.id = :projectId AND t.status = :status")
    Long countByStatus(@Param("projectId") Long projectId, @Param("status") String status);
}