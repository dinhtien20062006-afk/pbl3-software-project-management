package com.pbl3.dto.request;

import lombok.*;
import java.time.LocalDateTime;
import com.pbl3.entity.Task;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskRequest {
    private Long projectId;
    private Long teamId; // BỔ SUNG: ID của nhóm
    private String taskName;
    private String description;
    private LocalDateTime deadline;
    private Task.TaskPriority priority;
    private Task.TaskStatus status;
    private Long assigneeId;

    // Du lieu Member gui khi yeu cau doi/gia han cong viec
    private LocalDateTime requestedDeadline;
    private Long requestedAssigneeId;
}