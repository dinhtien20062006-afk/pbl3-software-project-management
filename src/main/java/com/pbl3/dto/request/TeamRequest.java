package com.pbl3.dto.request;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamRequest {
    private Long projectId;
    private String teamName;
    private String description;
    private LocalDateTime deadline;
    private Long leaderId;
    private LocalDateTime requestedDeadline;
    private String deadlineRequestReason;
}