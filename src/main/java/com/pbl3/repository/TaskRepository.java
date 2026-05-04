package com.pbl3.repository;

import com.pbl3.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByProjectId(Long projectId);
    List<Task> findByProjectIdAndAssigneeId(Long projectId, Long assigneeId);
    long countByProjectIdAndStatus(Long projectId, String status);
}