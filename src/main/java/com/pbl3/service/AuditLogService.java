package com.pbl3.service;

import com.pbl3.dto.response.AuditLogResponse;
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
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;

    // --- HELPER: Lấy User hiện tại ---
    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    // --- 1. Ghi Log (Dùng cho các Service khác gọi vào) ---
    @Transactional
    public void log(Project project, User currentUser, AuditLog.AuditActionType action, String targetName) {
        
        AuditLog log = AuditLog.builder()
                .project(project)
                .user(currentUser)
                .actionType(action)
                .targetName(targetName)
                .createdAt(LocalDateTime.now())
                .build();
        
        auditLogRepository.save(log);
    }

    // --- 2. Lấy Log theo Project (Dành cho Manager & Member) ---
    public List<AuditLogResponse> getLogsByProject(Long projectId) {
        User currentUser = getCurrentUser();

        // Kiểm tra project tồn tại
        if (!projectRepository.existsById(projectId)) {
            throw new AppException(ErrorCode.PROJECT_NOT_EXISTED);
        }

        // PHÂN QUYỀN: Nếu không phải ADMIN, phải là thành viên dự án mới được xem log dự án đó
        if (currentUser.getRole() != User.Role.ADMIN) {
            boolean isMember = projectMemberRepository.existsByProjectIdAndUserIdAndLeftAtIsNull(projectId, currentUser.getId());
            if (!isMember) {
                throw new AppException(ErrorCode.UNAUTHORIZED);
            }
        }

        return auditLogRepository.findAllByProjectId(projectId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // --- 3. Lấy toàn bộ Log hệ thống (Chỉ dành cho ADMIN) ---
    public List<AuditLogResponse> getAllSystemLogs() {
        User currentUser = getCurrentUser();

        if (currentUser.getRole() != User.Role.ADMIN) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        return auditLogRepository.findAllLogs().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // --- MAPPING & DỊCH THUẬT ---
    private AuditLogResponse mapToResponse(AuditLog log) {
        return AuditLogResponse.builder()
                .time(log.getCreatedAt())
                .executor(log.getUser().getUsername())
                .action(translateAction(log.getActionType()))
                .target(log.getTargetName())
                .build();
    }

    private String translateAction(AuditLog.AuditActionType type) {
        return switch (type) {
            case CREATE_PROJECT -> "Tạo dự án";
            case UPDATE_PROJECT -> "Cập nhật dự án";
            case DELETE_PROJECT -> "Xóa dự án";
            case ADD_MEMBER -> "Thêm thành viên";
            case REMOVE_MEMBER -> "Gỡ thành viên";
            case LEAVE_PROJECT -> "Rời dự án";
            case CREATE_TASK -> "Tạo công việc";
            case UPDATE_TASK -> "Sửa công việc";
            case SUBMIT_TASK -> "Nộp công việc";
            case REVIEW_TASK -> "Duyệt công việc";
            case START_TASK -> "Bắt đầu công việc";
            case COMPLETE_TASK -> "Hoàn thành công việc";
            case REQUEST_CHANGES -> "Yêu cầu thay đổi";
            default -> type.name();
        };
    }

}