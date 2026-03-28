package com.pbl3.dto.request;

import lombok.Data;

@Data
public class ProjectMemberRequest {
    private Long projectId;
    private Long userId;
    private String projectRole;
}