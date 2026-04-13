package com.pbl3.dto.request;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskUpdateRequest {
    private String taskName;
    private String description;
    private String priority;
    private String status;
    private LocalDateTime startDate; // Thêm cho khớp SQL
    private LocalDateTime deadline;                             
}