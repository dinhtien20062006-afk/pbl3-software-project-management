package com.pbl3.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ProjectMemberResponse {

    private Long userId;

    private String name;

    private String role;

    private LocalDateTime joinedAt;

    private LocalDateTime leftAt; 
}


   