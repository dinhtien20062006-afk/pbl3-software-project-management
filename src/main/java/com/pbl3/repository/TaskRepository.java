package com.pbl3.repository;

import com.pbl3.entity.Task;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    String TASK_GRAPH = "project,project.manager,projectTeam,projectTeam.leader,assignee,requestedAssignee";

    @Override
    @EntityGraph(attributePaths = {"project", "project.manager", "projectTeam", "projectTeam.leader", "assignee", "requestedAssignee"})
    Optional<Task> findById(Long id);

    @EntityGraph(attributePaths = {"project", "projectTeam", "assignee", "requestedAssignee"})
    List<Task> findByProjectTeamIdAndAssigneeId(Long teamId, Long userId);

    @EntityGraph(attributePaths = {"project", "project.manager", "projectTeam", "projectTeam.leader", "assignee", "requestedAssignee"})
    List<Task> findByProjectTeamId(Long teamId);

    long countByStatus(Task.TaskStatus status);

    // Dem task theo trang thai cho PM trong cac project duoc quan ly.
    long countByStatusAndProjectIdIn(Task.TaskStatus status, List<Long> projectIds);

    // Dem task theo trang thai cho ca nhan Member.
    long countByStatusAndAssigneeId(Task.TaskStatus status, Long assigneeId);

    @EntityGraph(attributePaths = {"project", "projectTeam", "assignee"})
    List<Task> findTop5ByOrderByDeadlineAsc();

    @EntityGraph(attributePaths = {"project", "projectTeam", "assignee"})
    List<Task> findTop5ByProjectIdInOrderByDeadlineAsc(List<Long> projectIds);

    @EntityGraph(attributePaths = {"project", "projectTeam", "assignee"})
    List<Task> findTop5ByAssigneeIdOrderByDeadlineAsc(Long assigneeId);

    // Loc task theo trang thai, do uu tien, nguoi duoc giao trong nhom.
    // EntityGraph nap san assignee/requestedAssignee de mapToResponse khong bi N+1 query.
    @EntityGraph(attributePaths = {"project", "project.manager", "projectTeam", "projectTeam.leader", "assignee", "requestedAssignee"})
    @Query("SELECT t FROM Task t WHERE t.projectTeam.id = :teamId " +
           "AND (:status IS NULL OR t.status = :status) " +
           "AND (:priority IS NULL OR t.priority = :priority) " +
           "AND (:assigneeId IS NULL OR t.assignee.id = :assigneeId)")
    List<Task> filterTeamTasks(@Param("teamId") Long teamId,
                               @Param("status") Task.TaskStatus status,
                               @Param("priority") Task.TaskPriority priority,
                               @Param("assigneeId") Long assigneeId);

    // --- TRUY VAN CHO CAP DO NHOM (TEAM) ---
    long countByProjectTeamId(Long teamId);
    long countByProjectTeamIdAndStatus(Long teamId, Task.TaskStatus status);
    long countByProjectTeamIdAndAssigneeId(Long teamId, Long assigneeId);
    long countByProjectTeamIdAndAssigneeIdAndStatus(Long teamId, Long assigneeId, Task.TaskStatus status);

    default long countOverdueTasksByTeamAndUser(Long teamId, Long userId) {
        return countByProjectTeamIdAndAssigneeIdAndStatus(teamId, userId, Task.TaskStatus.OVERDUE);
    }

    @EntityGraph(attributePaths = {"project", "projectTeam", "assignee"})
    List<Task> findByProjectTeamIdAndDeadlineBeforeAndStatusNotIn(
            Long teamId, LocalDateTime now, List<Task.TaskStatus> excludedStatuses);

    // --- TRUY VAN CHO CAP DO DU AN (PROJECT) ---
    long countByProjectId(Long projectId);
    long countByProjectIdAndStatus(Long projectId, Task.TaskStatus status);
}
