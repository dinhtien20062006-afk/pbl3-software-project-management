package com.pbl3.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ProjectMemberResponse {

    private Long projectId;
    private Long userId;

    private String projectName;
    private String username;

    private String projectRole;
    private LocalDateTime joinedAt;
}
