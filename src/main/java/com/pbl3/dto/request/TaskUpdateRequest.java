package com.pbl3.dto.request;

import lombok.*;

import java.time.LocalDate;
import com.pbl3.entity.Task;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskUpdateRequest {
    private String taskName;
    private Long assigneeId;
    private String description;
    private Task.TaskPriority priority;
    private Task.TaskStatus status;
    private LocalDate startDate;
    private LocalDate deadline;                             
}