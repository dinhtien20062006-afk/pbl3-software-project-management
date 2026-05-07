package com.pbl3.dto.request;

import lombok.*;
import java.time.LocalDate;
import com.pbl3.entity.Task;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskCreateRequest {
    private Long projectId;
    private Long teamId; // BỔ SUNG: ID của nhóm
    private String taskName;
    private String description;
    private LocalDate deadline;
    private Task.TaskPriority priority;
    private Long assigneeId;
}