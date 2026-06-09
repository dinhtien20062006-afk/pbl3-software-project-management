package com.pbl3.dto.response;

import com.pbl3.entity.ProjectTeam;
import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
public class TeamResponse {
    private Long projectId;
    private Long teamId;
    private String teamName;
    private String description;
    private String leaderName;
    private String leaderUsername;
    private Long leaderId;
    private String managerName;
    private String managerUsername;
    private LocalDateTime deadline;
    private LocalDateTime requestedDeadline;
    private String deadlineRequestReason;
    private ProjectTeam.TeamStatus status;
}