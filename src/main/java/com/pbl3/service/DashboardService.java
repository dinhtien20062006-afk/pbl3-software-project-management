package com.pbl3.service;

import com.pbl3.dto.response.DashboardResponse;
import com.pbl3.entity.*;
import com.pbl3.exception.AppException;
import com.pbl3.exception.ErrorCode;
import com.pbl3.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final ProjectTeamRepository projectTeamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TaskRepository taskRepository;

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    @Transactional(readOnly = true)
    public DashboardResponse getDashboardData() {
        User currentUser = getCurrentUser();
        User.Role role = currentUser.getRole();

        DashboardResponse.DashboardResponseBuilder builder = DashboardResponse.builder().role(role.name());

        switch (role) {
            case ADMIN -> buildAdminDashboard(builder);
            case PROJECT_MANAGER -> buildProjectManagerDashboard(builder, currentUser.getId());
            case MEMBER -> buildMemberDashboard(builder, currentUser.getId());
            default -> throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        return builder.build();
    }

    // --- CASE 1: DASHBOARD CHO ADMIN ---
    private void buildAdminDashboard(DashboardResponse.DashboardResponseBuilder builder) {
        Map<String, Long> metrics = new HashMap<>();
        metrics.put("totalUsers", userRepository.count());
        metrics.put("totalProjects", projectRepository.count());
        metrics.put("totalTeams", projectTeamRepository.count());
        metrics.put("totalTasks", taskRepository.count());
        builder.metrics(metrics);

        // Biểu đồ tổng quan Task toàn hệ thống
        Map<String, Long> statusOverview = new HashMap<>();
        for (Task.TaskStatus status : Task.TaskStatus.values()) {
            statusOverview.put(status.name(), taskRepository.countByStatus(status));
        }
        builder.taskStatusOverview(statusOverview);

        // Top 5 task hạn gần nhất hệ thống
        List<DashboardResponse.DashboardTaskItem> recentTasks = taskRepository.findTop5ByOrderByDeadlineAsc().stream()
                .map(this::mapToTaskItem)
                .collect(Collectors.toList());
        builder.recentTasks(recentTasks);
    }

    // --- CASE 2: DASHBOARD CHO PROJECT_MANAGER ---
    private void buildProjectManagerDashboard(DashboardResponse.DashboardResponseBuilder builder, Long pmId) {
        // Lấy danh sách các project do PM này quản lý
        List<Project> managedProjects = projectRepository.findAvailableProjects(pmId).stream()
                .filter(p -> p.getManager().getId().equals(pmId))
                .collect(Collectors.toList());
        List<Long> projectIds = managedProjects.stream().map(Project::getId).collect(Collectors.toList());

        Map<String, Long> metrics = new HashMap<>();
        metrics.put("myProjects", (long) managedProjects.size());
        metrics.put("myTeams", projectTeamRepository.countByProjectManagerId(pmId));
        metrics.put("totalMembersInCharge", teamMemberRepository.countUniqueMembersByManagerId(pmId));
        
        if (!projectIds.isEmpty()) {
            metrics.put("pendingApprovalTasks", taskRepository.countByStatusAndProjectIdIn(Task.TaskStatus.PENDING_APPROVAL, projectIds));
        } else {
            metrics.put("pendingApprovalTasks", 0L);
        }
        builder.metrics(metrics);

        // Biểu đồ trạng thái task trong các project của PM này
        Map<String, Long> statusOverview = new HashMap<>();
        for (Task.TaskStatus status : Task.TaskStatus.values()) {
            long count = projectIds.isEmpty() ? 0L : taskRepository.countByStatusAndProjectIdIn(status, projectIds);
            statusOverview.put(status.name(), count);
        }
        builder.taskStatusOverview(statusOverview);

        // 5 task hạn gần nhất thuộc quyền quản lý
        List<DashboardResponse.DashboardTaskItem> recentTasks = Collections.emptyList();
        if (!projectIds.isEmpty()) {
            recentTasks = taskRepository.findTop5ByProjectIdInOrderByDeadlineAsc(projectIds).stream()
                    .map(this::mapToTaskItem)
                    .collect(Collectors.toList());
        }
        builder.recentTasks(recentTasks);
    }

    // --- CASE 3: DASHBOARD CHO MEMBER ---
    private void buildMemberDashboard(DashboardResponse.DashboardResponseBuilder builder, Long memberId) {
        Map<String, Long> metrics = new HashMap<>();
        metrics.put("myTodoTasks", taskRepository.countByStatusAndAssigneeId(Task.TaskStatus.TODO, memberId));
        metrics.put("myInProgressTasks", taskRepository.countByStatusAndAssigneeId(Task.TaskStatus.IN_PROGRESS, memberId));
        metrics.put("myDoneTasks", taskRepository.countByStatusAndAssigneeId(Task.TaskStatus.DONE, memberId));
        builder.metrics(metrics);

        // Biểu đồ cá nhân
        Map<String, Long> statusOverview = new HashMap<>();
        for (Task.TaskStatus status : Task.TaskStatus.values()) {
            statusOverview.put(status.name(), taskRepository.countByStatusAndAssigneeId(status, memberId));
        }
        builder.taskStatusOverview(statusOverview);

        // Top 5 task cá nhân sắp đến hạn
        List<DashboardResponse.DashboardTaskItem> recentTasks = taskRepository.findTop5ByAssigneeIdOrderByDeadlineAsc(memberId).stream()
                .map(this::mapToTaskItem)
                .collect(Collectors.toList());
        builder.recentTasks(recentTasks);
    }

    // --- MAPPING HELPER ---
    private DashboardResponse.DashboardTaskItem mapToTaskItem(Task task) {
        return DashboardResponse.DashboardTaskItem.builder()
                .id(task.getId())
                .taskName(task.getTaskName())
                .projectName(task.getProject() != null ? task.getProject().getProjectName() : "N/A")
                .teamName(task.getProjectTeam() != null ? task.getProjectTeam().getTeamName() : "Dự án chung")
                .status(task.getStatus().name())
                .priority(task.getPriority() != null ? task.getPriority().name() : "MEDIUM")
                .deadline(task.getDeadline())
                .build();
    }
}