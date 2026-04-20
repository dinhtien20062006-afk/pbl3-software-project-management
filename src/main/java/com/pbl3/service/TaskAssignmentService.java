package com.pbl3.service;

import com.pbl3.dto.request.AuditLogRequest;
import com.pbl3.dto.request.TaskAssignmentRequest;
import com.pbl3.dto.response.TaskAssignmentResponse;
import com.pbl3.entity.*;
import com.pbl3.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskAssignmentService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final TaskAssignmentRepository assignmentRepository;
    private final AuditLogService auditLogService;

    //  lấy user login
    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext()
                .getAuthentication().getName();

        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));
    }

    // ================= ASSIGN =================
    public TaskAssignmentResponse assign(TaskAssignmentRequest request) {

        Task task = taskRepository.findById(request.getTaskId())
                .orElseThrow(() -> new RuntimeException("Task không tồn tại"));

        User manager = getCurrentUser();

        //  chỉ manager mới được assign
        if (!task.getProject().getManager().getId().equals(manager.getId())) {
            throw new RuntimeException("Bạn không có quyền giao task");
        }

        User assignee = userRepository.findById(request.getAssigneeId())
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        TaskAssignment assignment = TaskAssignment.builder()
                .task(task)
                .assignee(assignee)
                .assignedBy(manager)
                .assignedAt(LocalDateTime.now())
                .status(AssignmentStatus.ACTIVE)
                .build();

        assignmentRepository.save(assignment);

        //  audit log
        auditLogService.createLog(
                AuditLogRequest.builder()
                        .entityType("TASK")
                        .entityId(task.getId())
                        .userId(manager.getId())
                        .actionType("ASSIGN_TASK")
                        .oldValue("N/A")
                        .newValue(assignee.getUsername())
                        .build()
        );

        return mapToResponse(assignment);
    }

    // ================= UNASSIGN =================
    public void unassign(Long assignmentId) {

        TaskAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new RuntimeException("Assignment không tồn tại"));

        User manager = getCurrentUser();

        if (!assignment.getTask().getProject().getManager().getId().equals(manager.getId())) {
            throw new RuntimeException("Không có quyền");
        }

        assignment.setStatus(AssignmentStatus.REMOVED);
        assignmentRepository.save(assignment);

        auditLogService.createLog(
                AuditLogRequest.builder()
                        .entityType("TASK")
                        .entityId(assignment.getTask().getId())
                        .userId(manager.getId())
                        .actionType("REMOVE_ASSIGNMENT")
                        .oldValue(assignment.getAssignee().getUsername())
                        .newValue("Removed")
                        .build()
        );
    }

    public void reassign(Long assignmentId, Long newUserId) {

        TaskAssignment old = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new RuntimeException("Không tồn tại"));

        User manager = getCurrentUser();

        //  check quyền
        if (!old.getTask().getProject().getManager().getId().equals(manager.getId())) {
            throw new RuntimeException("Không có quyền");
        }

        // remove cũ
        old.setStatus(AssignmentStatus.REMOVED);
        assignmentRepository.save(old);

        // assign mới
        User newUser = userRepository.findById(newUserId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        TaskAssignment newAssign = TaskAssignment.builder()
                .task(old.getTask())
                .assignee(newUser)
                .assignedBy(manager)
                .assignedAt(LocalDateTime.now())
                .status(AssignmentStatus.ACTIVE)
                .build();

        assignmentRepository.save(newAssign);

        // log
        auditLogService.createLog(
                AuditLogRequest.builder()
                        .entityType("TASK")
                        .entityId(old.getTask().getId())
                        .userId(manager.getId())
                        .actionType("ASSIGN_TASK")
                        .oldValue(old.getAssignee().getUsername())
                        .newValue(newUser.getUsername())
                        .build()
        );
    }

    // ================= HISTORY =================
    public List<TaskAssignmentResponse> getHistory(Long taskId) {

        return assignmentRepository.findByTaskId(taskId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // ================= MAP =================
    private TaskAssignmentResponse mapToResponse(TaskAssignment a) {
        return TaskAssignmentResponse.builder()
                .taskId(a.getTask().getId())
                .taskName(a.getTask().getTaskName())
                .assignee(a.getAssignee().getUsername())
                .assignedBy(a.getAssignedBy().getUsername())
                .status(a.getStatus().name())
                .assignedAt(a.getAssignedAt())
                .build();
    }
}