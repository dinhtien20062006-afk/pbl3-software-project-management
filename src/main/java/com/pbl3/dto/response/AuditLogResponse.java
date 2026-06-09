package com.pbl3.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @Builder
public class AuditLogResponse {
    private LocalDateTime time;      // Thời gian
    private String executor;         // Người thực hiện (Full Name)
    private String action;           // Hành động (Tiếng Việt)
    private String target;           // Đối tượng (Tên Project/Task)
}