package com.pbl3.dto.response;

import lombok.*;
import java.time.LocalDateTime;
import com.pbl3.entity.Task;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShowTaskResponse { 
    private Long id;
    private String teamName;
    private String taskName;
    private String assigneeUsername;
    private String assigneeFullName;
    private String description;
    private Task.TaskStatus status;  
    private Task.TaskPriority priority; 
    private LocalDateTime deadLine;
    private String requestReason;
}