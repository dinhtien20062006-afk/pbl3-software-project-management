package com.pbl3.dto.response;

import lombok.*;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor @AllArgsConstructor
public class TeamMemberResponse {
    private Long teamMemberId;
    private Long userId;
    private String fullName;
    private String username;
    private String role;
    private LocalDate joinedAt;
}