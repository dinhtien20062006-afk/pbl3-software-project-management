package com.pbl3.repository;

import com.pbl3.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByProjectId(Long projectId);
    List<Task> findByProjectIdAndAssigneeId(Long projectId, Long assigneeId);
    long countByProjectIdAndStatus(Long projectId, String status);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.project.id = :projectId AND t.status = :status")
    Long countByStatus(@Param("projectId") Long projectId, @Param("status") String status);

    // 1. Dành cho Admin: Thống kê toàn bộ Task hệ thống
    @Query("SELECT t.status, COUNT(t) FROM Task t GROUP BY t.status")
    List<Object[]> countAllTasksGroupByStatus();

    // 2. Dành cho Manager: Thống kê Task trong các dự án họ quản lý
    @Query("SELECT t.status, COUNT(t) FROM Task t WHERE t.project.manager.id = :managerId GROUP BY t.status")
    List<Object[]> countTasksByManagerProjects(@Param("managerId") Long managerId);

    // 3. Dành cho Member: Thống kê Task cá nhân được giao
    @Query("SELECT t.status, COUNT(t) FROM Task t WHERE t.assignee.id = :userId GROUP BY t.status")
    List<Object[]> countPersonalTasksGroupByStatus(@Param("userId") Long userId);

    // 4. Đếm số lượng Task cụ thể (Cho thẻ Stat)
    @Query("SELECT COUNT(t) FROM Task t WHERE t.project.manager.id = :managerId AND t.status = 'PENDING_APPROVAL'")
    long countPendingTasksForManager(@Param("managerId") Long managerId);

    // 5. Đếm số lượng Task được giao cho một thành viên cụ thể
    @Query("SELECT COUNT(t) FROM Task t WHERE t.assignee.id = :assigneeId")
    long countByAssigneeId(@Param("assigneeId") Long assigneeId);
}