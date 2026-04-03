package com.pbl3.repository;

import com.pbl3.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    // Tìm tất cả các task thuộc về một project cụ thể
    List<Task> findByProjectId(Long projectId);

    // Spring sẽ tự hiểu và map với Enum TaskStatus của bạn
    List<Task> findByStatus(com.pbl3.entity.TaskStatus status);

    // Tìm các task có độ ưu tiên cao
    List<Task> findByPriority(com.pbl3.entity.TaskPriority priority);
}
