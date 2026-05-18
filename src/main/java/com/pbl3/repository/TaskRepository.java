package com.pbl3.repository;

import com.pbl3.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
       
    List<Task> findByProjectTeamIdAndAssigneeId(Long teamId, Long userId);
    List<Task> findByProjectTeamId(Long teamId);

    long countByStatus(Task.TaskStatus status);

// Đếm task theo trạng thái cho PM (các task thuộc dự án PM quản lý)
long countByStatusAndProjectIdIn(Task.TaskStatus status, List<Long> projectIds);

// Đếm task theo trạng thái cho cá nhân Member (được giao)
long countByStatusAndAssigneeId(Task.TaskStatus status, Long assigneeId);

// Top 5 task mới cập nhật gần đây để hiển thị bảng tin (Activity Log/Recent Tasks)
List<Task> findTop5ByOrderByDeadlineAsc();
List<Task> findTop5ByProjectIdInOrderByDeadlineAsc(List<Long> projectIds);
List<Task> findTop5ByAssigneeIdOrderByDeadlineAsc(Long assigneeId);

    // Lọc task theo trạng thái, độ ưu tiên, người được giao trong nhóm
    @Query("SELECT t FROM Task t WHERE t.projectTeam.id = :teamId " +
           "AND (:status IS NULL OR t.status = :status) " +
           "AND (:priority IS NULL OR t.priority = :priority) " +
           "AND (:assigneeId IS NULL OR t.assignee.id = :assigneeId) ")
    List<Task> filterTeamTasks(@Param("teamId") Long teamId,
                               @Param("status") Task.TaskStatus status,
                               @Param("priority") Task.TaskPriority priority,
                               @Param("assigneeId") Long assigneeId);

        // --- TRUY VẤN CHO CẤP ĐỘ NHÓM (TEAM) ---
    long countByProjectTeamId(Long teamId);
    long countByProjectTeamIdAndStatus(Long teamId, Task.TaskStatus status);
    long countByProjectTeamIdAndAssigneeId(Long teamId, Long assigneeId);
    long countByProjectTeamIdAndAssigneeIdAndStatus(Long teamId, Long assigneeId, Task.TaskStatus status);

    // Đếm số task quá hạn của 1 thành viên trong 1 nhóm cụ thể (Deadline < Hiện tại và chưa DONE)
    @Query("SELECT COUNT(t) FROM Task t WHERE t.projectTeam.id = :teamId AND t.assignee.id = :userId " +
        "AND t.deadline < :now AND t.status != 'DONE'")
    long countOverdueTasksByTeamAndUser(@Param("teamId") Long teamId, @Param("userId") Long userId, @Param("now") LocalDateTime now);

    // --- TRUY VẤN CHO CẤP ĐỘ DỰ ÁN (PROJECT) ---
    long countByProjectId(Long projectId);
    long countByProjectIdAndStatus(Long projectId, Task.TaskStatus status);
}
