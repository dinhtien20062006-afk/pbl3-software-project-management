package com.pbl3.service;

import com.pbl3.dto.response.AuditLogResponse;
import com.pbl3.dto.response.DashboardResponse;
import com.pbl3.dto.response.ProjectResponse;
import com.pbl3.entity.Project;
import com.pbl3.entity.User;
import com.pbl3.exception.AppException;
import com.pbl3.exception.ErrorCode;
import com.pbl3.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final TaskRepository taskRepository;
    private final AuditLogRepository auditLogRepository;

    // Helper: Lấy thông tin User đang đăng nhập
    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    @Transactional(readOnly = true)
    public DashboardResponse getDashboardData() {
        User user = getCurrentUser();
        DashboardResponse.DashboardResponseBuilder responseBuilder = DashboardResponse.builder();
        DashboardResponse.Statistics.StatisticsBuilder statsBuilder = DashboardResponse.Statistics.builder();

        // 1. Phân quyền xử lý logic lấy dữ liệu (Polymorphic Logic by Role)
        switch (user.getRole()) {
            case ADMIN -> buildAdminDashboard(statsBuilder, responseBuilder);
            case PROJECT_MANAGER -> buildManagerDashboard(user, statsBuilder, responseBuilder);
            case MEMBER -> buildMemberDashboard(user, statsBuilder, responseBuilder);
        }

        return responseBuilder.stats(statsBuilder.build()).build();
    }

    /**
     * LOGIC CHO ADMIN: Thống kê toàn hệ thống
     */
    private void buildAdminDashboard(DashboardResponse.Statistics.StatisticsBuilder stats, 
                                    DashboardResponse.DashboardResponseBuilder response) {
        // Stats: Tổng quát
        stats.totalUsers(userRepository.countTotalUsers());
        stats.totalProjects(projectRepository.count());
        stats.totalTasks(taskRepository.count());

        // Charts: Phân bổ trạng thái dự án toàn hệ thống
        response.projectDistribution(convertListToMap(projectRepository.countProjectsGroupByStatus()));
        
        // Charts: Trạng thái Task toàn hệ thống
        response.taskDistribution(convertListToMap(taskRepository.countAllTasksGroupByStatus()));

        // Grids: 10 hoạt động mới nhất toàn sàn
        response.recentActivities(getRecentLogs(null, true));
    }

    /**
     * LOGIC CHO MANAGER: Tập trung vào dự án quản lý và công việc cần duyệt
     */
    private void buildManagerDashboard(User user, DashboardResponse.Statistics.StatisticsBuilder stats, 
                                       DashboardResponse.DashboardResponseBuilder response) {
        Long managerId = user.getId();

        // Stats: Dự án đang quản lý và Task cần duyệt (Pending)
        stats.totalProjects(projectRepository.countByManagerId(managerId));
        stats.pendingTasks(taskRepository.countPendingTasksForManager(managerId));
        
        // Charts: Phân bổ trạng thái Task trong các dự án mình quản lý
        response.taskDistribution(convertListToMap(taskRepository.countTasksByManagerProjects(managerId)));

        // Grids: Danh sách dự án đang chạy
        List<Project> activeProjects = projectRepository.findActiveProjectsByManager(managerId);
        response.activeProjects(activeProjects.stream().map(this::mapProjectToResponse).toList());

        // Grids: Hoạt động trong các dự án mình tham gia
        response.recentActivities(getRecentLogs(user.getId(), false));
    }

    /**
     * LOGIC CHO MEMBER: Tập trung vào task cá nhân và dự án tham gia
     */
    private void buildMemberDashboard(User user, DashboardResponse.Statistics.StatisticsBuilder stats, 
                                      DashboardResponse.DashboardResponseBuilder response) {
        Long memberId = user.getId();

        // Stats: Số dự án tham gia và số Task cá nhân
        stats.totalProjects(projectMemberRepository.countActiveJoinedProjects(memberId));
        stats.totalTasks(taskRepository.countByAssigneeId(memberId));

        // Charts: Tiến độ công việc cá nhân (TODO, IN_PROGRESS, DONE...)
        response.taskDistribution(convertListToMap(taskRepository.countPersonalTasksGroupByStatus(memberId)));

        // Grids: Danh sách dự án tham gia
        List<Project> joinedProjects = projectMemberRepository.findProjectsByMemberId(memberId);
        response.activeProjects(joinedProjects.stream().map(this::mapProjectToResponse).toList());

        // Grids: Hoạt động gần đây của các đồng nghiệp trong dự án
        response.recentActivities(getRecentLogs(user.getId(), false));
    }

    // --- UTILS & MAPPING ---

    /**
     * Chuyển đổi List<Object[]> từ Repository (SQL Group By) sang Map<String, Long>
     * Object[0] là key (Status), Object[1] là value (Count)
     */
    private Map<String, Long> convertListToMap(List<Object[]> list) {
        return list.stream().collect(Collectors.toMap(
                obj -> obj[0].toString(),
                obj -> (Long) obj[1]
        ));
    }

    /**
     * Lấy danh sách log gần nhất (Sử dụng PageRequest để giới hạn 10 bản ghi)
     */
    private List<AuditLogResponse> getRecentLogs(Long userId, boolean isAdmin) {
        List<com.pbl3.entity.AuditLog> logs;
        PageRequest limitTen = PageRequest.of(0, 10);

        if (isAdmin) {
            logs = auditLogRepository.findRecentLogs(limitTen);
        } else {
            logs = auditLogRepository.findRecentLogsByUserProjects(userId, limitTen);
        }

        return logs.stream().map(log -> AuditLogResponse.builder()
                .time(log.getCreatedAt())
                .executor(log.getUser().getFullName())
                .action(log.getActionType().name()) // Bạn có thể dùng hàm translateAction đã viết ở AuditLogService
                .target(log.getTargetName())
                .build()).toList();
    }

    private ProjectResponse mapProjectToResponse(Project project) {
        return ProjectResponse.builder()
                .id(project.getId())
                .projectName(project.getProjectName())
                .status(project.getStatus())
                .startDate(project.getStartDate())
                .endDate(project.getEndDate())
                .managerName(project.getManager().getFullName())
                .build();
    }
}