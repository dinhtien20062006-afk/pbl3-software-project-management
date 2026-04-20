package com.pbl3.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TaskAssignmentRequest {

    private Long taskId;

    private Long assigneeId;
}