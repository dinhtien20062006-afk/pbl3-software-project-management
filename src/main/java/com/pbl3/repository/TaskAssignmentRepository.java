package com.pbl3.repository;

import com.pbl3.entity.TaskAssignment;
import com.pbl3.entity.AssignmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskAssignmentRepository extends JpaRepository<TaskAssignment, Long> {

    List<TaskAssignment> findByTaskId(Long taskId);

    List<TaskAssignment> findByTaskIdAndStatus(Long taskId, AssignmentStatus status);
}