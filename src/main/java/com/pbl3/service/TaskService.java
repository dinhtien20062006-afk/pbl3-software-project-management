package com.pbl3.service;

import com.pbl3.dto.request.TaskRequest;
import com.pbl3.dto.response.ShowTaskResponse;
import com.pbl3.entity.*;
import com.pbl3.exception.AppException;
import com.pbl3.exception.ErrorCode;
import com.pbl3.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final ProjectTeamRepository projectTeamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    // --- HELPER QUYEN HAN CAP NHAT ---

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }


    private void validateProjectIsEditable(Project project) {
        if (project.getStatus() == Project.ProjectStatus.ON_HOLD
                || project.getStatus() == Project.ProjectStatus.COMPLETED) {
            throw new AppException(ErrorCode.INVALID_ASSIGNMENT);
        }
    }

    // Kiem tra quyen: Manager du an hoac Leader cua nhom moi duoc phep tao/sua/duyet task
    private void validateManagementPrivilege(Long projectId, Long teamId, User user) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new AppException(ErrorCode.PROJECT_NOT_EXISTED));

        // 1. Neu la Manager du an -> Co quyen
        if (project.getManager().getId().equals(user.getId())) return;

        // 2. Neu co teamId, kiem tra xem user co phai Leader cua Team do khong
        if (teamId != null) {
            ProjectTeam team = projectTeamRepository.findById(teamId)
                    .orElseThrow(() -> new AppException(ErrorCode.TEAM_NOT_EXISTED));
            if (team.getLeader().getId().equals(user.getId())) return;
        }

        throw new AppException(ErrorCode.UNAUTHORIZED);
    }

    @Transactional
    public ShowTaskResponse createTask(TaskRequest request) {
        User currentUser = getCurrentUser();

        // Neu UI khong truyen projectId nhung co truyen teamId thi tu lay projectId tu team
        if (request.getProjectId() == null && request.getTeamId() != null) {
            ProjectTeam team = projectTeamRepository.findById(request.getTeamId())
                    .orElseThrow(() -> new AppException(ErrorCode.TEAM_NOT_EXISTED));
            request.setProjectId(team.getProject().getId());
        }

        validateManagementPrivilege(request.getProjectId(), request.getTeamId(), currentUser);

        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new AppException(ErrorCode.PROJECT_NOT_EXISTED));
        validateProjectIsEditable(project);
        ProjectTeam team = (request.getTeamId() != null)
                ? projectTeamRepository.findById(request.getTeamId())
                    .orElseThrow(() -> new AppException(ErrorCode.TEAM_NOT_EXISTED))
                : null;

        validateTaskDeadline(request.getDeadline(), team);

        User assignee = null;
        if (request.getAssigneeId() != null) {
            assignee = userRepository.findById(request.getAssigneeId())
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

            if (team != null) {
                boolean isMemberOfTeam = teamMemberRepository.existsByProjectTeamIdAndUserId(team.getId(), assignee.getId());
                if (!isMemberOfTeam) throw new AppException(ErrorCode.USER_NOT_IN_TEAM);
            }
        }

        Task task = Task.builder()
                .taskName(request.getTaskName())
                .description(request.getDescription())
                .deadline(request.getDeadline())
                .priority(request.getPriority())
                .status(Task.TaskStatus.TODO)
                .project(project)
                .projectTeam(team)
                .assignee(assignee)
                .requestReason(null)
                .requestedDeadline(null)
                .requestedAssignee(null)
                .build();

        auditLogService.log(project, currentUser, AuditLog.AuditActionType.CREATE_TASK, task.getTaskName());
        return mapToResponse(taskRepository.save(task));
    }

    @Transactional
    public ShowTaskResponse updateTask(Long taskId, TaskRequest request) {
        User currentUser = getCurrentUser();
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new AppException(ErrorCode.TASK_NOT_EXISTED));

        validateManagementPrivilege(task.getProject().getId(),
                task.getProjectTeam() != null ? task.getProjectTeam().getId() : null,
                currentUser);
        validateProjectIsEditable(task.getProject());

        if (request.getTaskName() != null) task.setTaskName(request.getTaskName());
        if (request.getDescription() != null) task.setDescription(request.getDescription());
        if (request.getPriority() != null) task.setPriority(request.getPriority());
        if (request.getStatus() != null) task.setStatus(request.getStatus());

        if (request.getDeadline() != null) {
            validateTaskDeadline(request.getDeadline(), task.getProjectTeam());
            task.setDeadline(request.getDeadline());
        }

        // Thay doi nguoi thuc hien (reassign)
        if (request.getAssigneeId() != null) {
            User newAssignee = userRepository.findById(request.getAssigneeId())
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

            if (task.getProjectTeam() != null) {
                boolean isMemberOfTeam = teamMemberRepository.existsByProjectTeamIdAndUserId(
                        task.getProjectTeam().getId(), newAssignee.getId());
                if (!isMemberOfTeam) throw new AppException(ErrorCode.USER_NOT_IN_TEAM);
            }
            task.setAssignee(newAssignee);
        }

        clearMemberRequest(task);
        auditLogService.log(task.getProject(), currentUser, AuditLog.AuditActionType.UPDATE_TASK, task.getTaskName());
        return mapToResponse(taskRepository.save(task));
    }

    private void autoUpdateOverdueTasks(Long teamId) {
        ProjectTeam team = projectTeamRepository.findById(teamId)
                .orElseThrow(() -> new AppException(ErrorCode.TEAM_NOT_EXISTED));
        if ((team.getProject().getStatus() == Project.ProjectStatus.ON_HOLD || team.getProject().getStatus() == Project.ProjectStatus.COMPLETED)) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        List<Task> overdueTasks = taskRepository.findByProjectTeamIdAndDeadlineBeforeAndStatusNotIn(
                teamId, now, List.of(Task.TaskStatus.DONE, Task.TaskStatus.OVERDUE));

        if (!overdueTasks.isEmpty()) {
            overdueTasks.forEach(task -> task.setStatus(Task.TaskStatus.OVERDUE));
            taskRepository.saveAll(overdueTasks);
        }
    }

    // Lay danh sach task cua mot Team (chi thanh vien cua Team hoac Manager du an moi duoc xem)
    @Transactional
    public List<ShowTaskResponse> getTeamTasks(Long teamId) {
        User currentUser = getCurrentUser();
        ProjectTeam team = projectTeamRepository.findById(teamId)
                .orElseThrow(() -> new AppException(ErrorCode.TEAM_NOT_EXISTED));

        boolean isMember = teamMemberRepository.existsByProjectTeamIdAndUserId(teamId, currentUser.getId());
        if (!isMember && !team.getProject().getManager().getId().equals(currentUser.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        autoUpdateOverdueTasks(teamId);

        return taskRepository.findByProjectTeamId(teamId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // Xoa task (chi Manager du an hoac Leader nhom moi duoc xoa)
    @Transactional
    public void deleteTask(Long taskId) {
        User currentUser = getCurrentUser();
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new AppException(ErrorCode.TASK_NOT_EXISTED));

        validateManagementPrivilege(task.getProject().getId(),
                task.getProjectTeam() != null ? task.getProjectTeam().getId() : null,
                currentUser);
        validateProjectIsEditable(task.getProject());

        taskRepository.delete(task);
        auditLogService.log(task.getProject(), currentUser, AuditLog.AuditActionType.DELETE_TASK, task.getTaskName());
    }

    @Transactional
    public void startTask(Long taskId) {
        User currentUser = getCurrentUser();
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new AppException(ErrorCode.TASK_NOT_EXISTED));
        validateProjectIsEditable(task.getProject());

        if (task.getAssignee() == null || !task.getAssignee().getId().equals(currentUser.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        if (task.getProjectTeam() != null && task.getProjectTeam().getStatus() != ProjectTeam.TeamStatus.IN_PROGRESS) {
            throw new AppException(ErrorCode.TEAM_NOT_STARTED);
        }

        if (task.getStatus() != Task.TaskStatus.TODO) {
            throw new AppException(ErrorCode.INVALID_ASSIGNMENT);
        }

        task.setStatus(Task.TaskStatus.IN_PROGRESS);
        taskRepository.save(task);
        auditLogService.log(task.getProject(), currentUser, AuditLog.AuditActionType.START_TASK, task.getTaskName());
    }

    @Transactional
    public void submitTask(Long taskId) {
        User currentUser = getCurrentUser();
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new AppException(ErrorCode.TASK_NOT_EXISTED));
        validateProjectIsEditable(task.getProject());

        if (task.getAssignee() == null || !task.getAssignee().getId().equals(currentUser.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        if (task.getStatus() != Task.TaskStatus.IN_PROGRESS) {
            throw new AppException(ErrorCode.INVALID_ASSIGNMENT);
        }

        task.setStatus(Task.TaskStatus.PENDING_APPROVAL);
        clearMemberRequest(task);
        taskRepository.save(task);
        auditLogService.log(task.getProject(), currentUser, AuditLog.AuditActionType.SUBMIT_TASK, task.getTaskName());
    }

    @Transactional
    public void reviewTask(Long taskId, boolean approved) {
        User currentUser = getCurrentUser();
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new AppException(ErrorCode.TASK_NOT_EXISTED));

        validateManagementPrivilege(task.getProject().getId(),
                task.getProjectTeam() != null ? task.getProjectTeam().getId() : null,
                currentUser);
        validateProjectIsEditable(task.getProject());

        if (task.getStatus() != Task.TaskStatus.PENDING_APPROVAL) {
            throw new AppException(ErrorCode.INVALID_ASSIGNMENT);
        }

        if (approved) {
            task.setStatus(Task.TaskStatus.DONE);
            auditLogService.log(task.getProject(), currentUser, AuditLog.AuditActionType.REVIEW_TASK, task.getTaskName());
        } else {
            task.setStatus(Task.TaskStatus.IN_PROGRESS);
            auditLogService.log(task.getProject(), currentUser, AuditLog.AuditActionType.REQUEST_CHANGES, task.getTaskName());
        }
        taskRepository.save(task);
        if (approved) {
            autoCompleteTeamAndProjectIfNeeded(task);
        }
    }

    private void autoCompleteTeamAndProjectIfNeeded(Task task) {
        if (task.getProjectTeam() != null) {
            Long teamId = task.getProjectTeam().getId();
            long totalTeamTasks = taskRepository.countByProjectTeamId(teamId);
            long doneTeamTasks = taskRepository.countByProjectTeamIdAndStatus(teamId, Task.TaskStatus.DONE);

            if (totalTeamTasks > 0 && totalTeamTasks == doneTeamTasks) {
                ProjectTeam team = task.getProjectTeam();
                team.setStatus(ProjectTeam.TeamStatus.COMPLETED);
                projectTeamRepository.save(team);
            }
        }

        Long projectId = task.getProject().getId();
        long totalProjectTasks = taskRepository.countByProjectId(projectId);
        long doneProjectTasks = taskRepository.countByProjectIdAndStatus(projectId, Task.TaskStatus.DONE);

        if (totalProjectTasks > 0 && totalProjectTasks == doneProjectTasks) {
            Project project = task.getProject();
            project.setStatus(Project.ProjectStatus.COMPLETED);
            projectRepository.save(project);
        }
    }

    // YEU CAU DOI TASK: Member gui ly do va nguoi thuc hien moi mong muon
    @Transactional
    public void requestChangeTask(Long taskId, String reason, Long requestedAssigneeId) {
        User currentUser = getCurrentUser();
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new AppException(ErrorCode.TASK_NOT_EXISTED));
        validateProjectIsEditable(task.getProject());

        if (task.getAssignee() == null || !task.getAssignee().getId().equals(currentUser.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        if (task.getStatus() != Task.TaskStatus.IN_PROGRESS) {
            throw new AppException(ErrorCode.INVALID_ASSIGNMENT);
        }

        if (reason == null || reason.isBlank()) {
            throw new AppException(ErrorCode.INVALID_ASSIGNMENT);
        }

        if (requestedAssigneeId == null) {
            throw new AppException(ErrorCode.INVALID_ASSIGNMENT);
        }

        User requestedAssignee = userRepository.findById(requestedAssigneeId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        if (task.getProjectTeam() != null) {
            boolean isMemberOfTeam = teamMemberRepository.existsByProjectTeamIdAndUserId(
                    task.getProjectTeam().getId(), requestedAssignee.getId());
            if (!isMemberOfTeam) throw new AppException(ErrorCode.USER_NOT_IN_TEAM);
        }

        task.setStatus(Task.TaskStatus.CHANGE_REQUESTED);
        task.setRequestReason(reason);
        task.setRequestedAssignee(requestedAssignee);
        task.setRequestedDeadline(null);
        taskRepository.save(task);

        auditLogService.log(task.getProject(), currentUser, AuditLog.AuditActionType.CHANGE_TASK, task.getTaskName());
    }

    // YEU CAU GIA HAN: Member gui ly do va deadline moi mong muon
    @Transactional
    public void requestExtension(Long taskId, String reason, LocalDateTime requestedDeadline) {
        User currentUser = getCurrentUser();
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new AppException(ErrorCode.TASK_NOT_EXISTED));
        validateProjectIsEditable(task.getProject());

        if (task.getAssignee() == null || !task.getAssignee().getId().equals(currentUser.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        if (task.getStatus() != Task.TaskStatus.IN_PROGRESS) {
            throw new AppException(ErrorCode.INVALID_ASSIGNMENT);
        }

        if (reason == null || reason.isBlank()) {
            throw new AppException(ErrorCode.INVALID_ASSIGNMENT);
        }

        validateTaskDeadline(requestedDeadline, task.getProjectTeam());

        task.setStatus(Task.TaskStatus.EXTENSION_REQUESTED);
        task.setRequestReason(reason);
        task.setRequestedDeadline(requestedDeadline);
        task.setRequestedAssignee(null);
        taskRepository.save(task);

        auditLogService.log(task.getProject(), currentUser, AuditLog.AuditActionType.EXTEND_DEADLINE, task.getTaskName());
    }

    // Leader/PM xac nhan hoac tu choi yeu cau doi task/gia han cua Member
    @Transactional
    public void resolveMemberRequest(Long taskId, boolean approved) {
        User currentUser = getCurrentUser();
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new AppException(ErrorCode.TASK_NOT_EXISTED));

        validateManagementPrivilege(task.getProject().getId(),
                task.getProjectTeam() != null ? task.getProjectTeam().getId() : null,
                currentUser);
        validateProjectIsEditable(task.getProject());

        if (task.getStatus() != Task.TaskStatus.CHANGE_REQUESTED &&
                task.getStatus() != Task.TaskStatus.EXTENSION_REQUESTED) {
            throw new AppException(ErrorCode.INVALID_ASSIGNMENT);
        }

        if (approved) {
            if (task.getStatus() == Task.TaskStatus.EXTENSION_REQUESTED) {
                validateTaskDeadline(task.getRequestedDeadline(), task.getProjectTeam());
                task.setDeadline(task.getRequestedDeadline());
                task.setStatus(Task.TaskStatus.IN_PROGRESS);
            } else if (task.getStatus() == Task.TaskStatus.CHANGE_REQUESTED) {
                if (task.getRequestedAssignee() == null) {
                    throw new AppException(ErrorCode.INVALID_ASSIGNMENT);
                }
                task.setAssignee(task.getRequestedAssignee());
                // Khi doi nguoi thuc hien, dua task ve TODO de nguoi moi bat dau lai.
                task.setStatus(Task.TaskStatus.TODO);
            }

            auditLogService.log(task.getProject(), currentUser, AuditLog.AuditActionType.UPDATE_TASK, task.getTaskName());
        } else {
            task.setStatus(Task.TaskStatus.IN_PROGRESS);
            auditLogService.log(task.getProject(), currentUser, AuditLog.AuditActionType.REQUEST_CHANGES, task.getTaskName());
        }

        clearMemberRequest(task);
        taskRepository.save(task);
    }

    @Transactional
    public List<ShowTaskResponse> filterTeamTasks(Long teamId, Task.TaskStatus status,
                                                  Task.TaskPriority priority, Long assigneeId) {

        autoUpdateOverdueTasks(teamId);
        return taskRepository.filterTeamTasks(teamId, status, priority, assigneeId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private void validateTaskDeadline(LocalDateTime taskDeadline, ProjectTeam team) {
        if (taskDeadline == null) {
            throw new AppException(ErrorCode.INVALID_DEADLINE);
        }

        if (team != null && team.getDeadline() != null && taskDeadline.isAfter(team.getDeadline())) {
            throw new AppException(ErrorCode.INVALID_DEADLINE);
        }
    }

    private void clearMemberRequest(Task task) {
        task.setRequestReason(null);
        task.setRequestedDeadline(null);
        task.setRequestedAssignee(null);
    }

    private ShowTaskResponse mapToResponse(Task task) {
        return ShowTaskResponse.builder()
                .id(task.getId())
                .taskName(task.getTaskName())
                .description(task.getDescription())
                .status(task.getStatus())
                .priority(task.getPriority())
                .deadLine(task.getDeadline())
                .requestReason(task.getRequestReason())
                .requestedDeadline(task.getRequestedDeadline())
                .requestedAssigneeUsername(task.getRequestedAssignee() != null ? task.getRequestedAssignee().getUsername() : null)
                .requestedAssigneeFullName(task.getRequestedAssignee() != null ? task.getRequestedAssignee().getFullName() : null)
                .assigneeUsername(task.getAssignee() != null ? task.getAssignee().getUsername() : "Chưa phân công")
                .assigneeFullName(task.getAssignee() != null ? task.getAssignee().getFullName() : "Chưa phân công")
                .teamName(task.getProjectTeam() != null ? task.getProjectTeam().getTeamName() : "N/A")
                .build();
    }
}
