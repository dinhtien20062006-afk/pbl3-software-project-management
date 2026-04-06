package com.pbl3.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProjectMemberResponse {

    private Long userId;

    private String name;

    private String role;
}
