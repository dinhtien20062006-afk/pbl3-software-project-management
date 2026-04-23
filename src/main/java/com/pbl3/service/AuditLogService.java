package com.pbl3.service;

import com.pbl3.dto.request.AuditLogRequest;
import com.pbl3.dto.response.AuditLogResponse;
import com.pbl3.entity.AuditLog;
import com.pbl3.entity.User;
import com.pbl3.exception.AppException;
import com.pbl3.exception.ErrorCode;
import com.pbl3.entity.AuditActionType;
import com.pbl3.repository.AuditLogRepository;
import com.pbl3.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    // CREATE LOG (từ request)
    public AuditLogResponse createLog(AuditLogRequest request) {

        AuditLog log = AuditLog.builder()
                .entityType(request.getEntityType())
                .entityId(request.getEntityId())
                .userId(request.getUserId())
                .actionType(parseAction(request.getActionType()))
                .oldValue(request.getOldValue())
                .newValue(request.getNewValue())
                .createdAt(LocalDateTime.now())
                .build();

        auditLogRepository.save(log);

        return mapToResponse(log);
    }

    // GET LOG BY TASK
    public List<AuditLogResponse> getLogsByTask(Long taskId) {

        return auditLogRepository
                .findByEntityTypeAndEntityIdOrderByCreatedAtDesc("TASK", taskId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // GET LOG BY USER
    public List<AuditLogResponse> getLogsByUser(Long userId) {

        return auditLogRepository
                .findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ================== PRIVATE ==================

    // mapping
    private AuditLogResponse mapToResponse(AuditLog log) {

        String username = userRepository.findById(log.getUserId())
                .map(User::getUsername)
                .orElse("Không rõ");

        return AuditLogResponse.builder()
                .id(log.getId())
                .action(translateAction(log.getActionType().name()))
                .description(buildDescription(log, username))
                .oldValue(log.getOldValue())
                .newValue(log.getNewValue())
                .username(username)
                .createdAt(log.getCreatedAt())
                .build();
    }

    // parse enum an toàn
    private AuditActionType parseAction(String action) {
        try {
            return AuditActionType.valueOf(action);
        } catch (Exception e) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }
    }

    // translate sang tiếng Việt
    private String translateAction(String action) {
        switch (action) {
            case "CREATE_TASK": return "Tạo công việc";
            case "UPDATE_TASK": return "Cập nhật công việc";
            case "DELETE_TASK": return "Xóa công việc";

            case "ASSIGN_TASK": return "Phân công công việc";
            case "REMOVE_ASSIGNMENT": return "Hủy phân công";

            case "ADD_COMMENT": return "Thêm bình luận";

            case "CREATE_PROJECT": return "Tạo dự án";
            case "UPDATE_PROJECT": return "Cập nhật dự án";
            case "DELETE_PROJECT": return "Xóa dự án";

            case "ADD_MEMBER": return "Thêm thành viên";
            case "REMOVE_MEMBER": return "Xóa thành viên";
            case "LEAVE_PROJECT": return "Rời khỏi dự án";

            default: return action;
        }
    }

    // mô tả tiếng Việt
    private String buildDescription(AuditLog log, String username) {

        switch (log.getActionType().name()) {

            case "CREATE_TASK":
                return username + " đã tạo một công việc mới";

            case "UPDATE_TASK":
               return username + " đã cập nhật công việc";

            case "DELETE_TASK":
                return username + " đã xóa một công việc";

            case "ASSIGN_TASK":
                return username + " đã giao công việc cho " + log.getNewValue();

            case "REMOVE_ASSIGNMENT":
                return username + " đã hủy giao công việc từ " + log.getOldValue();

            case "ADD_COMMENT":
                return username + " đã thêm một bình luận";

            case "CREATE_PROJECT":
                return username + " đã tạo một dự án";

            case "UPDATE_PROJECT":
                return username + " đã cập nhật dự án";

            case "DELETE_PROJECT":
                return username + " đã xóa một dự án";

            case "ADD_MEMBER":
                return username + " đã thêm " + log.getNewValue() + " vào dự án";

            case "REMOVE_MEMBER":
                return username + " đã xóa " + log.getOldValue() + " khỏi dự án";
            case "LEAVE_PROJECT":
                return username + " đã rời khỏi dự án";

            default:
                return username + " đã thực hiện " + log.getActionType().name();
        }
    }
}