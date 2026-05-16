package com.pbl3.dto.response;

import com.pbl3.entity.ProjectTeam;
import lombok.*;
import java.time.LocalDate;

@Data
@Builder
public class TeamResponse {
    private Long teamId;
    private String teamName;
    private String description;
    private String leaderName;
    private Long leaderId;
    private String managerName;    
    private LocalDate deadline;
    private ProjectTeam.TeamStatus status;
}