package com.pbl3.service;

import com.pbl3.dto.request.TaskCreateRequest;
import com.pbl3.dto.request.TaskUpdateRequest;
import com.pbl3.dto.response.ShowTaskResponse;
import com.pbl3.entity.*;
import com.pbl3.exception.AppException;
import com.pbl3.exception.ErrorCode;
import com.pbl3.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final ProjectTeamRepository projectTeamRepository; 
    private final UserRepository userRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final AuditLogService auditLogService;

    // --- HELPER QUYỀN HẠN CẬP NHẬT ---

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    /**
     * Kiểm tra quyền quản lý: Trả về true nếu là Manager dự án hoặc Leader của Team đó
     */
    private void validateManagementPrivilege(Long projectId, Long teamId, User user) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new AppException(ErrorCode.PROJECT_NOT_EXISTED));

        // 1. Nếu là Manager dự án -> Có quyền
        if (project.getManager().getId().equals(user.getId())) return;

        // 2. Nếu có teamId, kiểm tra xem user có phải là Leader của Team đó không
        if (teamId != null) {
            ProjectTeam team = projectTeamRepository.findById(teamId)
                    .orElseThrow(() -> new AppException(ErrorCode.TEAM_NOT_EXISTED));
            if (team.getLeader().getId().equals(user.getId())) return;
        }

        throw new AppException(ErrorCode.UNAUTHORIZED);
    }

    @Transactional
    public ShowTaskResponse createTask(TaskCreateRequest request) {
        User currentUser = getCurrentUser();
        
        // Kiểm tra quyền: Manager dự án HOẶC Leader của Team mới được tạo task
        validateManagementPrivilege(request.getProjectId(), request.getTeamId(), currentUser);

        Project project = projectRepository.findById(request.getProjectId()).get();
        ProjectTeam team = (request.getTeamId() != null) ? 
                projectTeamRepository.findById(request.getTeamId()).orElse(null) : null;

        // Kiểm tra deadline task phải nằm trong deadline dự án và deadline team
        validateTaskDeadline(request.getDeadline(), project, team);

        User assignee = null;
        if (request.getAssigneeId() != null) {
            assignee = userRepository.findById(request.getAssigneeId())
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
            
            // Nếu có teamId, chỉ được giao cho thành viên trong Team đó
            if (team != null) {
                boolean isMemberOfTeam = projectMemberRepository.existsByTeamIdAndUserId(team.getId(), assignee.getId());
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
                .projectTeam(team) // Gán nhóm
                .assignee(assignee)
                .build();

        auditLogService.log(project, currentUser, AuditLog.AuditActionType.CREATE_TASK, task.getTaskName());
        return mapToResponse(taskRepository.save(task));
    }

    @Transactional
    public ShowTaskResponse updateTask(Long taskId, TaskUpdateRequest request) {
        User currentUser = getCurrentUser();
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new AppException(ErrorCode.TASK_NOT_EXISTED));

        // Quyền sửa: Manager dự án hoặc Leader của nhóm chứa task đó
        validateManagementPrivilege(task.getProject().getId(), 
                task.getProjectTeam() != null ? task.getProjectTeam().getId() : null, 
                currentUser);

        if (request.getTaskName() != null) task.setTaskName(request.getTaskName());
        if (request.getDescription() != null) task.setDescription(request.getDescription());
        if (request.getDeadline() != null) task.setDeadline(request.getDeadline());
        if (request.getPriority() != null) task.setPriority(request.getPriority());
        if (request.getStatus() != null) task.setStatus(request.getStatus());

        // Nếu deadline được cập nhật, kiểm tra lại tính hợp lệ
        if (request.getDeadline() != null) {
            validateTaskDeadline(request.getDeadline(), task.getProject(), task.getProjectTeam());
        }

        // Thay đổi người thực hiện (Reassign)
        if (request.getAssigneeId() != null) {
            User newAssignee = userRepository.findById(request.getAssigneeId())
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
                // Nếu task thuộc nhóm, chỉ được giao cho thành viên trong nhóm đó
            if (task.getProjectTeam() != null) {
                boolean isMemberOfTeam = projectMemberRepository.existsByTeamIdAndUserId(task.getProjectTeam().getId(), newAssignee.getId());
                if (!isMemberOfTeam) throw new AppException(ErrorCode.USER_NOT_IN_TEAM);
            }
            task.setAssignee(newAssignee);
        }
        
        auditLogService.log(task.getProject(), currentUser, AuditLog.AuditActionType.UPDATE_TASK, task.getTaskName());
        return mapToResponse(taskRepository.save(task));
    }

    /**
     * BỔ SUNG: Lấy tất cả task trong một Team (Cho Member xem chéo nhau)
     */
    @Transactional(readOnly = true)
    public List<ShowTaskResponse> getTeamTasks(Long teamId) {
        User currentUser = getCurrentUser();
        ProjectTeam team = projectTeamRepository.findById(teamId)
                .orElseThrow(() -> new AppException(ErrorCode.TEAM_NOT_EXISTED));

        // Bảo mật: Chỉ thành viên của Team mới được xem
        boolean isMember = projectMemberRepository.existsByTeamIdAndUserId(teamId, currentUser.getId());
        if (!isMember && !team.getProject().getManager().getId().equals(currentUser.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        return taskRepository.findByProjectTeamId(teamId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void startTask(Long taskId) {
        User currentUser = getCurrentUser();
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new AppException(ErrorCode.TASK_NOT_EXISTED));

        // Kiểm tra quyền Assignee
        if (!task.getAssignee().getId().equals(currentUser.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        // RÀNG BUỘC: Nhóm phải ở trạng thái IN_PROGRESS thì member mới được start task
        if (task.getProjectTeam() != null && task.getProjectTeam().getStatus() != ProjectTeam.TeamStatus.IN_PROGRESS) {
            throw new AppException(ErrorCode.TEAM_NOT_STARTED);
        }

        if (task.getStatus() == Task.TaskStatus.TODO) {
            task.setStatus(Task.TaskStatus.IN_PROGRESS);
            taskRepository.save(task);
            auditLogService.log(task.getProject(), currentUser, AuditLog.AuditActionType.START_TASK, task.getTaskName());
        }
    }

    @Transactional
    public void submitTask(Long taskId) {
        User currentUser = getCurrentUser();
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new AppException(ErrorCode.TASK_NOT_EXISTED));

        // CHỈ người được giao task này mới có quyền Submit
        if (task.getAssignee() == null || !task.getAssignee().getId().equals(currentUser.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        if (task.getStatus() == Task.TaskStatus.IN_PROGRESS) {
            task.setStatus(Task.TaskStatus.PENDING_APPROVAL);
            taskRepository.save(task);
            auditLogService.log(task.getProject(), currentUser, AuditLog.AuditActionType.SUBMIT_TASK, task.getTaskName());
        }
    }

    @Transactional
    public void reviewTask(Long taskId, boolean approved) {
        User currentUser = getCurrentUser();
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new AppException(ErrorCode.TASK_NOT_EXISTED));

        // Manager dự án hoặc Leader nhóm mới được duyệt task
        validateManagementPrivilege(task.getProject().getId(), 
                task.getProjectTeam() != null ? task.getProjectTeam().getId() : null, 
                currentUser);

        if(approved) {
            task.setStatus(Task.TaskStatus.DONE);
            auditLogService.log(task.getProject(), currentUser, AuditLog.AuditActionType.REVIEW_TASK, task.getTaskName());
        } else {
            task.setStatus(Task.TaskStatus.IN_PROGRESS);
            auditLogService.log(task.getProject(), currentUser, AuditLog.AuditActionType.REQUEST_CHANGES, task.getTaskName());
        }
        taskRepository.save(task);
    }

    // YÊU CẦU ĐỔI TASK (Member gửi yêu cầu)
    @Transactional
    public void requestChangeTask(Long taskId, String reason) {
        User currentUser = getCurrentUser();
        Task task = taskRepository.findById(taskId).get();
        
        if (!task.getAssignee().getId().equals(currentUser.getId())) throw new AppException(ErrorCode.UNAUTHORIZED);

        task.setStatus(Task.TaskStatus.CHANGE_REQUESTED);
        taskRepository.save(task);
        
        auditLogService.log(task.getProject(), currentUser, AuditLog.AuditActionType.UPDATE_TASK, "Yêu cầu đổi task: " + reason);
    }

    // YÊU CẦU GIA HẠN (Member gửi yêu cầu)
    @Transactional
    public void requestExtension(Long taskId, String reason) {
        User currentUser = getCurrentUser();
        Task task = taskRepository.findById(taskId).get();
        
        if (!task.getAssignee().getId().equals(currentUser.getId())) throw new AppException(ErrorCode.UNAUTHORIZED);

        task.setStatus(Task.TaskStatus.EXTENSION_REQUESTED);
        taskRepository.save(task);
        
        auditLogService.log(task.getProject(), currentUser, AuditLog.AuditActionType.UPDATE_TASK, "Yêu cầu gia hạn task: " + reason);
    }

    //Lấy danh sách task sắp hết hạn trong nhóm (deadline trong vòng X ngày) - Dành cho Leader và Manager
    public List<ShowTaskResponse> getUrgentTasksByTeam(Long teamId, Integer daysThreshold) {
        LocalDate now = LocalDate.now();
        LocalDate limitDate = now.plusDays(daysThreshold);

        // Bảo mật: Chỉ Leader của Team hoặc Manager dự án mới được xem danh sách này
        User currentUser = getCurrentUser();
        ProjectTeam team = projectTeamRepository.findById(teamId)
                .orElseThrow(() -> new AppException(ErrorCode.TEAM_NOT_EXISTED));

        // Kiểm tra quyền xem của người dùng hiện tại
        if (!team.getLeader().getId().equals(currentUser.getId()) && !team.getProject().getManager().getId().equals(currentUser.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        // Gọi repo đã có ORDER BY deadline ASC
        return taskRepository.findUrgentTasksByTeam(teamId, now, limitDate).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<ShowTaskResponse> filterTeamTasks(Long teamId, Task.TaskStatus status, 
                                                  Task.TaskPriority priority, Long assigneeId) {
        
        return taskRepository.filterTeamTasks(teamId, status, priority, assigneeId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private void validateTaskDeadline(LocalDate taskDeadline, Project project, ProjectTeam team) {
        if (taskDeadline == null) return;
        // So với dự án
        if (project.getEndDate() != null && taskDeadline.isAfter(project.getEndDate())) {
            throw new AppException(ErrorCode.INVALID_DEADLINE);
        }
        // So với nhóm (nếu có)
        if (team != null && team.getDeadline() != null && taskDeadline.isAfter(team.getDeadline())) {
            throw new AppException(ErrorCode.INVALID_DEADLINE);
        }
    }

    private ShowTaskResponse mapToResponse(Task task) {
        return ShowTaskResponse.builder()
                .id(task.getId())
                .taskName(task.getTaskName())
                .description(task.getDescription())
                .status(task.getStatus())
                .priority(task.getPriority())
                .deadLine(task.getDeadline())
                .assigneeUsername(task.getAssignee() != null ? task.getAssignee().getUsername() : "Chưa phân công")
                .teamName(task.getProjectTeam() != null ? task.getProjectTeam().getTeamName() : "N/A") // BỔ SUNG
                .build();
    }
}