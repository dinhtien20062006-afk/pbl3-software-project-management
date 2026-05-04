package com.pbl3.service;

import com.pbl3.dto.request.ProjectMemberRequest;
import com.pbl3.dto.response.ProjectMemberResponse;
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
public class ProjectMemberService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final TaskRepository taskRepository;

    private final AuditLogService auditLogService;
    // Lấy user đang đăng nhập hiện tại
    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    // 1. Lấy danh sách thành viên ĐANG hoạt động trong dự án
    public List<ProjectMemberResponse> getMembers(Long projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new AppException(ErrorCode.PROJECT_NOT_EXISTED);
        }

        return projectMemberRepository.findByProjectIdAndLeftAtIsNull(projectId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // 2. Thêm thành viên (Chỉ Manager của dự án mới có quyền)
    @Transactional
    public void addMember(Long projectId, ProjectMemberRequest request) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new AppException(ErrorCode.PROJECT_NOT_EXISTED));

        User currentUser = getCurrentUser();
        
        // Kiểm tra quyền: Chỉ Manager của dự án mới được thêm người
        if (!project.getManager().getId().equals(currentUser.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        User userToAdd = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        if (projectMemberRepository.existsByProjectIdAndUserIdAndLeftAtIsNull(projectId, request.getUserId())) {
            throw new AppException(ErrorCode.MEMBER_ALREADY_EXISTED);
        }

        ProjectMember pm = ProjectMember.builder()
                .project(project)
                .user(userToAdd)
                .projectRole(request.getRole())
                .joinedAt(LocalDate.now())
                .build();

        projectMemberRepository.save(pm);
        auditLogService.log(project, currentUser, AuditLog.AuditActionType.ADD_MEMBER, userToAdd.getFullName());
    }

    // 3. Xóa thành viên (Đánh dấu leftAt - Chỉ Manager mới được xóa)
    @Transactional
    public void removeMember(Long projectId, Long userId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new AppException(ErrorCode.PROJECT_NOT_EXISTED));

        User currentUser = getCurrentUser();
        if (!project.getManager().getId().equals(currentUser.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        ProjectMember pm = projectMemberRepository.findByProjectIdAndUserIdAndLeftAtIsNull(projectId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // XỬ LÝ NHIỆM VỤ trước khi rời đi
        List<Task> assignedTasks = taskRepository.findByProjectIdAndAssigneeId(projectId, userId);
        assignedTasks.forEach(task -> task.setAssignee(null));
        taskRepository.saveAll(assignedTasks);

                pm.setLeftAt(LocalDate.now());

        auditLogService.log(project, currentUser, AuditLog.AuditActionType.REMOVE_MEMBER, pm.getUser().getFullName());
        projectMemberRepository.save(pm);
    }

    // 4. Tự rời dự án
    @Transactional
    public void leaveProject(Long projectId) {
        User currentUser = getCurrentUser();
        
        ProjectMember pm = projectMemberRepository.findByProjectIdAndUserIdAndLeftAtIsNull(projectId, currentUser.getId())
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED));

        // XỬ LÝ NHIỆM VỤ trước khi rời đi
        List<Task> assignedTasks = taskRepository.findByProjectIdAndAssigneeId(projectId, currentUser.getId());
        assignedTasks.forEach(task -> task.setAssignee(null));
        taskRepository.saveAll(assignedTasks);

        pm.setLeftAt(LocalDate.now());
        auditLogService.log(pm.getProject(), currentUser, AuditLog.AuditActionType.LEAVE_PROJECT, pm.getUser().getFullName());
        projectMemberRepository.save(pm);
    }

    private ProjectMemberResponse mapToResponse(ProjectMember pm) {
        return ProjectMemberResponse.builder()
                .userId(pm.getUser().getId())
                .fullName(pm.getUser().getFullName())
                .username(pm.getUser().getUsername())
                .role(pm.getProjectRole())
                .joinedAt(pm.getJoinedAt())
                .build();
    }

}