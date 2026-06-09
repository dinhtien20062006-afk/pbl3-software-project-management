package com.pbl3.dto.request;

import lombok.Data;

@Data
public class TeamMemberRequest {
    private Long userId;
    private String role;
}