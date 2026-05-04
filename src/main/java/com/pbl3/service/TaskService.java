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

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectMemberRepository projectMemberRepository;

    private final AuditLogService auditLogService;

    // --- HELPER QUYỀN HẠN ---

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    private void validateManagerPrivilege(Project project, User user) {
        if (!project.getManager().getId().equals(user.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
    }

    // Kiểm tra User có phải thành viên dự án không
    private void validateMemberInProject(Long projectId, Long userId) {
        if (!projectMemberRepository.existsByProjectIdAndUserIdAndLeftAtIsNull(projectId, userId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
    }

    // --- NGHIỆP VỤ CỐT LÕI (CRUD & LOGIC) ---

    @Transactional
    public ShowTaskResponse createTask(TaskCreateRequest request) {
        User currentUser = getCurrentUser();
        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new AppException(ErrorCode.PROJECT_NOT_EXISTED));

        // 1. Chỉ Manager mới được tạo task
        validateManagerPrivilege(project, currentUser);

        // 2. Kiểm tra người được giao (Assignee) có thuộc dự án không
        User assignee = null;
        if (request.getAssigneeId() != null) {
            assignee = userRepository.findById(request.getAssigneeId())
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
            validateMemberInProject(project.getId(), assignee.getId());
        }
        // 3. Kiểm tra deadline có hợp lệ không
         if (request.getDeadline() != null && (request.getDeadline().isAfter(project.getEndDate()) || request.getDeadline().isBefore(project.getStartDate()))) {
            throw new AppException(ErrorCode.INVALID_DEADLINE);
        }

        Task task = Task.builder()
                .taskName(request.getTaskName())
                .description(request.getDescription())
                .deadline(request.getDeadline())
                .priority(request.getPriority())
                .status(Task.TaskStatus.TODO)
                .project(project)
                .assignee(assignee) // Giao trực tiếp ở đây
                .build();

         auditLogService.log(project, currentUser, AuditLog.AuditActionType.CREATE_TASK, task.getTaskName());
                return mapToResponse(taskRepository.save(task));
    }

    @Transactional
    public ShowTaskResponse updateTask(Long taskId, TaskUpdateRequest request) {
        User currentUser = getCurrentUser();
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new AppException(ErrorCode.TASK_NOT_EXISTED));

        // Chỉ Manager của dự án đó mới có quyền sửa nội dung/giao việc
        validateManagerPrivilege(task.getProject(), currentUser);

        if (request.getTaskName() != null) task.setTaskName(request.getTaskName());
        if (request.getDescription() != null) task.setDescription(request.getDescription());
        if (request.getDeadline() != null) task.setDeadline(request.getDeadline());
        if (request.getPriority() != null) task.setPriority(request.getPriority());
        if (request.getStatus() != null) task.setStatus(request.getStatus());

        // Nếu deadline được cập nhật, kiểm tra lại tính hợp lệ
        if (request.getDeadline() != null && (request.getDeadline().isAfter(task.getProject().getEndDate()) || request.getDeadline().isBefore(task.getProject().getStartDate()))) {
            throw new AppException(ErrorCode.INVALID_DEADLINE);
        }

        // Thay đổi người thực hiện (Reassign)
        if (request.getAssigneeId() != null) {
            User newAssignee = userRepository.findById(request.getAssigneeId())
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
            validateMemberInProject(task.getProject().getId(), newAssignee.getId());
            task.setAssignee(newAssignee);
        }

            auditLogService.log(task.getProject(), currentUser, AuditLog.AuditActionType.UPDATE_TASK, task.getTaskName());
        return mapToResponse(taskRepository.save(task));
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

    /**
     * Hàm lọc đa năng: Theo Project, Trạng thái, Ưu tiên và Người thực hiện
     */
    @Transactional(readOnly = true)
    public List<ShowTaskResponse> filterTasks(Long projectId, Task.TaskStatus status, 
                                             Task.TaskPriority priority, Long assigneeId) {
        User currentUser = getCurrentUser();
        
        // Bảo mật: Phải là thành viên dự án mới được xem
        validateMemberInProject(projectId, currentUser.getId());

        // Gợi ý: Bạn nên sử dụng Specification trong JPA để xử lý lọc động tốt hơn,
        // nhưng đây là cách làm thủ công dễ hiểu cho PBL3:
        return taskRepository.findByProjectId(projectId).stream()
                .filter(t -> status == null || t.getStatus().equals(status))
                .filter(t -> priority == null || t.getPriority().equals(priority))
                .filter(t -> assigneeId == null || (t.getAssignee() != null && t.getAssignee().getId().equals(assigneeId)))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Lấy các task được giao cho chính mình trong một dự án
     */
    public List<ShowTaskResponse> getMyTasks(Long projectId) {
        User currentUser = getCurrentUser();
        return taskRepository.findByProjectIdAndAssigneeId(projectId, currentUser.getId()).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void reviewTask(Long taskId, boolean approved) {
        User currentUser = getCurrentUser();
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new AppException(ErrorCode.TASK_NOT_EXISTED));

        validateManagerPrivilege(task.getProject(), currentUser);
        // Nếu được duyệt, ghi log REVIEW_TASK cho Manager, COMPLETED_TASK CHO ASSIGNEE, CẬP NHẬT TRẠNG THÁI DONE
        if(approved) {
            task.setStatus(Task.TaskStatus.DONE);
            taskRepository.save(task);

            auditLogService.log(task.getProject(), currentUser, AuditLog.AuditActionType.REVIEW_TASK, task.getTaskName());
            if (task.getAssignee() != null) {
                auditLogService.log(task.getProject(), task.getAssignee(), AuditLog.AuditActionType.COMPLETE_TASK, task.getTaskName());
            }
        } else {
            // Nếu không được duyệt, ghi log REQUEST_CHANGES cho Manager, CẬP NHẬT TRẠNG THÁI IN_PROGRESS
            task.setStatus(Task.TaskStatus.IN_PROGRESS);
            taskRepository.save(task);
            auditLogService.log(task.getProject(), currentUser, AuditLog.AuditActionType.REQUEST_CHANGES, task.getTaskName());
        }
    }

    //Bắt đầu công việc cho phép người được giao chuyển trạng thái từ TODO -> IN_PROGRESS
    @Transactional
    public void startTask(Long taskId) {
        User currentUser = getCurrentUser();
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new AppException(ErrorCode.TASK_NOT_EXISTED));

        // CHỈ người được giao task này mới có quyền bắt đầu
        if (task.getAssignee() == null || !task.getAssignee().getId().equals(currentUser.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        if (task.getStatus() == Task.TaskStatus.TODO) {
            task.setStatus(Task.TaskStatus.IN_PROGRESS);
            taskRepository.save(task);
            auditLogService.log(task.getProject(), currentUser, AuditLog.AuditActionType.START_TASK, task.getTaskName());
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
                .assigneeUsername(task.getAssignee() != null ? task.getAssignee().getUsername() : "Unassigned")
                .build();
    }
}