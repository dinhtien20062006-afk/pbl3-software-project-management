package com.pbl3.dto.response;

import lombok.*;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class TeamResponse {
    private Long teamId;
    private String teamName;
    private String description;
    private String leaderName;
    private LocalDate deadline;
    private List<String> memberNames;
}