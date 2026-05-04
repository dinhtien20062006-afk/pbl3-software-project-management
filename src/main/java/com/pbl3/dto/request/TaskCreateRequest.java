package com.pbl3.dto.request;

import lombok.*;
import java.time.LocalDate;
import com.pbl3.entity.Task;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskCreateRequest {
    
    private String taskName;
    private String description;
    private Long projectId;
    private Long assigneeId; // Người được giao task
    private Task.TaskPriority priority; 
    private Task.TaskStatus status; 
    private LocalDate deadline;
}