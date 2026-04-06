package com.pbl3.dto.request;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskCreateRequest {
    
    private String taskName;
    
    private String description;
    
    // Gửi ID của project để map vào quan hệ @ManyToOne trong Entity
    private Long projectId;
    
    // Dùng String để Frontend dễ gửi, sau đó Service sẽ convert sang Enum
    private String priority; // LOW, MEDIUM, HIGH, URGENT
    
    private String status;
    
    private LocalDateTime deadline;
}