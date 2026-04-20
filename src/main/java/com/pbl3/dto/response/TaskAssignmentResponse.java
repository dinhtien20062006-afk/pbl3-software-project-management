package com.pbl3.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class TaskAssignmentResponse {

    private Long taskId;
    private String taskName;
  
    private String assignee;
    private String assignedBy;

    private String status;
    private LocalDateTime assignedAt;
}