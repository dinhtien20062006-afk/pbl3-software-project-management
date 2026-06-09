package com.pbl3.service;

import com.pbl3.dto.request.ProjectRequest;
import com.pbl3.dto.response.ProjectResponse;
import com.pbl3.entity.*;
import com.pbl3.exception.AppException;
import com.pbl3.exception.ErrorCode;
import com.pbl3.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final AuditLogService auditLogService;
    private final AuditLogRepository auditLogRepository;
    private final ProjectTeamRepository projectTeamRepository;
    private final TaskRepository taskRepository;
    // Lấy user hiện tại đang đăng nhập
    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }


    private boolean canManageProject(Project project, User user) {
        return user.getRole() == User.Role.ADMIN || project.getManager().getId().equals(user.getId());
    }

    private void normalizeProjectDates(Project project) {
        if (project.getStartDate() != null && project.getEndDate() != null
                && project.getEndDate().isBefore(project.getStartDate())) {
            project.setEndDate(project.getStartDate());
        }
    }

    private void normalizeRequestDates(ProjectRequest request) {
        if (request.getStartDate() != null && request.getEndDate() != null
                && request.getEndDate().isBefore(request.getStartDate())) {
            request.setEndDate(request.getStartDate());
        }
    }

    @Transactional
    public ProjectResponse createProject(ProjectRequest request) {
        User currentUser = getCurrentUser();

        // Chỉ PROJECT_MANAGER hoặc ADMIN mới có quyền tạo dự án
        if (currentUser.getRole() == User.Role.MEMBER) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        normalizeRequestDates(request);

        Project project = Project.builder()
                .projectName(request.getProjectName())
                .description(request.getDescription())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(Project.ProjectStatus.PLANNING)
                .manager(currentUser) // Người tạo là Manager chính
                .build();

        Project savedProject = projectRepository.save(project);
        
        auditLogService.log(savedProject, currentUser, AuditLog.AuditActionType.CREATE_PROJECT, project.getProjectName());
        
        return mapToResponse(savedProject);
    }

    @Transactional
    public ProjectResponse updateProject(Long id, ProjectRequest request) {
        User currentUser = getCurrentUser();
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PROJECT_NOT_EXISTED));

        // Kiểm tra quyền: Chỉ Manager của dự án hoặc ADMIN mới được sửa
        if (!canManageProject(project, currentUser)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        validateProjectIsEditable(project);

        if (request.getProjectName() != null) project.setProjectName(request.getProjectName());
        if (request.getDescription() != null) project.setDescription(request.getDescription());

        // Khi dự án đã IN_PROGRESS thì không cho sửa ngày bắt đầu nữa.
        // Chỉ dự án PLANNING mới được đổi startDate.
        if (request.getStartDate() != null && project.getStatus() == Project.ProjectStatus.PLANNING) {
            project.setStartDate(request.getStartDate());
        }
        if (request.getEndDate() != null) project.setEndDate(request.getEndDate());

        // Nếu ngày đến hạn bị nhập trước ngày bắt đầu thì tự chỉnh lại cho hợp lý,
        // tránh bắt lỗi cứng ở form khi người dùng đổi ngày.
        normalizeProjectDates(project);

        auditLogService.log(project, currentUser, AuditLog.AuditActionType.UPDATE_PROJECT, project.getProjectName());
        return mapToResponse(projectRepository.save(project));
    }

    @Transactional
    public void deleteProject(Long id) {
        User currentUser = getCurrentUser();
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PROJECT_NOT_EXISTED));

        // 1. Kiểm tra quyền
        if (!canManageProject(project, currentUser)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        // Cho phép xóa project kể cả khi đang ON_HOLD.
        // ON_HOLD chỉ khóa các thao tác chỉnh sửa/nghiệp vụ, không khóa thao tác xóa project.

        // Gửi project = null để bản ghi log DELETE này không bị xóa theo project
        auditLogService.log(
            null, 
            currentUser, 
            AuditLog.AuditActionType.DELETE_PROJECT, 
            project.getProjectName()
        );

        // 3. Ngắt kết nối các AuditLog hiện tại với Project này
        auditLogRepository.decoupleLogsFromProject(id);

        // 4. Thực hiện xóa Project
        projectRepository.delete(project);
    }

    @Transactional
    public List<ProjectResponse> getAllProjects() {
        refreshProjectStatuses();
        User currentUser = getCurrentUser();

        // Neu la ADMIN: thay tat ca project. Repository da nap san manager de tranh N+1.
        if (currentUser.getRole() == User.Role.ADMIN) {
            return projectRepository.findAll().stream()
                    .map(this::mapToResponse)
                    .toList();
        }

        // User thuong/PM: chi lay project co lien quan ngay tu Repository, khong findAll roi filter trong Java.
        return projectRepository.findAvailableProjects(currentUser.getId()).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public ProjectResponse getProjectById(Long id) {
        refreshProjectStatuses();
        User currentUser = getCurrentUser();
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PROJECT_NOT_EXISTED));

        if (currentUser.getRole() != User.Role.ADMIN && !isUserInProject(project, currentUser)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        return mapToResponse(project);
    }

    // Kiểm tra xem User có liên quan đến dự án (là Manager hoặc thành viên của bất kỳ Team nào trong dự án)
    private boolean isUserInProject(Project project, User user) {
    if (project.getManager().getId().equals(user.getId())) return true;
    return teamMemberRepository.existsByProjectTeam_ProjectIdAndUserId(project.getId(), user.getId());
    }

    // Tìm dự án theo tên (cho phép tìm kiếm một phần tên)
    @Transactional
    public List<ProjectResponse> getProjectsByName(String name) {
        refreshProjectStatuses();
        User currentUser = getCurrentUser();
        if (currentUser.getRole() == User.Role.ADMIN) {
            return projectRepository.findByProjectNameContainingIgnoreCase(name).stream()
                    .map(this::mapToResponse)
                    .toList();
        }

        return projectRepository.findAvailableProjectsByName(currentUser.getId(), name).stream()
                .map(this::mapToResponse)
                .toList();
    }


    @Transactional
    public ProjectResponse putProjectOnHold(Long id) {
        User currentUser = getCurrentUser();
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PROJECT_NOT_EXISTED));

        if (!canManageProject(project, currentUser)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        if (project.getStatus() == Project.ProjectStatus.COMPLETED) {
            throw new AppException(ErrorCode.INVALID_ASSIGNMENT);
        }

        project.setStatus(Project.ProjectStatus.ON_HOLD);
        Project savedProject = projectRepository.save(project);
        auditLogService.log(savedProject, currentUser, AuditLog.AuditActionType.UPDATE_PROJECT, project.getProjectName());
        return mapToResponse(savedProject);
    }


    @Transactional
    public ProjectResponse resumeProject(Long id) {
        return resumeProject(id, null);
    }

    @Transactional
    public ProjectResponse resumeProject(Long id, ProjectRequest request) {
        User currentUser = getCurrentUser();
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PROJECT_NOT_EXISTED));

        if (!canManageProject(project, currentUser)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        if (project.getStatus() != Project.ProjectStatus.ON_HOLD) {
            throw new AppException(ErrorCode.INVALID_ASSIGNMENT);
        }

        // Khi tiếp tục dự án ON_HOLD, cho phép cập nhật lại thông tin dự án trước.
        // Đây là ngoại lệ có kiểm soát, vì ON_HOLD bình thường bị khóa sửa.
        if (request != null) {
            normalizeRequestDates(request);
            if (request.getProjectName() != null) project.setProjectName(request.getProjectName());
            if (request.getDescription() != null) project.setDescription(request.getDescription());
            if (request.getStartDate() != null) project.setStartDate(request.getStartDate());
            if (request.getEndDate() != null) project.setEndDate(request.getEndDate());
            normalizeProjectDates(project);
        }

        LocalDateTime now = LocalDateTime.now();
        Project.ProjectStatus nextStatus =
                project.getStartDate() != null && project.getStartDate().isAfter(now)
                        ? Project.ProjectStatus.PLANNING
                        : Project.ProjectStatus.IN_PROGRESS;

        project.setStatus(nextStatus);
        Project savedProject = projectRepository.save(project);

        if (nextStatus == Project.ProjectStatus.IN_PROGRESS) {
            List<ProjectTeam> teams = projectTeamRepository.findByProjectId(project.getId());
            teams.forEach(team -> {
                if (team.getStatus() == ProjectTeam.TeamStatus.PLANNING) {
                    team.setStatus(ProjectTeam.TeamStatus.IN_PROGRESS);
                }
            });
            projectTeamRepository.saveAll(teams);
        }

        auditLogService.log(savedProject, currentUser, AuditLog.AuditActionType.UPDATE_PROJECT, project.getProjectName());
        autoCompleteProjectsWhenAllTasksDone();
        Project latestProject = projectRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PROJECT_NOT_EXISTED));
        return mapToResponse(latestProject);
    }

    @Transactional
    public ProjectResponse completeProject(Long id) {
        User currentUser = getCurrentUser();
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PROJECT_NOT_EXISTED));

        if (!canManageProject(project, currentUser)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        validateProjectIsEditable(project);

        validateAllProjectTasksDone(project.getId());

        project.setStatus(Project.ProjectStatus.COMPLETED);
        Project savedProject = projectRepository.save(project);
        auditLogService.log(savedProject, currentUser, AuditLog.AuditActionType.COMPLETE_PROJECT, project.getProjectName());
        return mapToResponse(savedProject);
    }

    @Transactional
    public void refreshProjectStatuses() {
        autoStartProjectsByStartTime();
        autoCompleteProjectsWhenAllTasksDone();
    }

    @Transactional
    public void autoStartProjectsByStartTime() {
        LocalDateTime now = LocalDateTime.now();
        List<Project> projects = projectRepository.findByStatusAndStartDateLessThanEqual(Project.ProjectStatus.PLANNING, now);
        if (projects.isEmpty()) {
            return;
        }

        projects.forEach(project -> project.setStatus(Project.ProjectStatus.IN_PROGRESS));
        projectRepository.saveAll(projects);
    }

    @Transactional
    public void autoCompleteProjectsWhenAllTasksDone() {
        List<Project> projects = projectRepository.findByStatus(Project.ProjectStatus.IN_PROGRESS);
        for (Project project : projects) {
            long totalTasks = taskRepository.countByProjectId(project.getId());
            if (totalTasks == 0) {
                continue;
            }

            long doneTasks = taskRepository.countByProjectIdAndStatus(project.getId(), Task.TaskStatus.DONE);
            if (totalTasks == doneTasks) {
                project.setStatus(Project.ProjectStatus.COMPLETED);
                projectRepository.save(project);
            }
        }
    }

    private void validateAllProjectTasksDone(Long projectId) {
        long totalTasks = taskRepository.countByProjectId(projectId);
        if (totalTasks == 0) {
            throw new AppException(ErrorCode.PROJECT_NOT_READY_TO_COMPLETE);
        }

        long doneTasks = taskRepository.countByProjectIdAndStatus(projectId, Task.TaskStatus.DONE);
        if (totalTasks != doneTasks) {
            throw new AppException(ErrorCode.PROJECT_NOT_READY_TO_COMPLETE);
        }
    }


    private void validateProjectIsEditable(Project project) {
        if (project.getStatus() == Project.ProjectStatus.ON_HOLD
                || project.getStatus() == Project.ProjectStatus.COMPLETED) {
            throw new AppException(ErrorCode.INVALID_ASSIGNMENT);
        }
    }

    private ProjectResponse mapToResponse(Project project) {
        return ProjectResponse.builder()
                .id(project.getId())
                .projectName(project.getProjectName())
                .description(project.getDescription())
                .startDate(project.getStartDate())
                .endDate(project.getEndDate())
                .status(project.getStatus())
                .managerName(project.getManager().getFullName())
                .managerUsername(project.getManager().getUsername())
                .build();
    }
}