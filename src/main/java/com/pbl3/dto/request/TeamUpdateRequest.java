package com.pbl3.dto.request;

import lombok.*;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamUpdateRequest {
    private String teamName;
    private String description;
    private LocalDate deadline;
    private Long leaderId; 
}
