package com.pbl3.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

import com.pbl3.entity.Project;

@Data
@AllArgsConstructor
@Builder
public class ProjectResponse {
    private Long id;
    private String projectName;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private Project.ProjectStatus status;
    private String managerName;
    private String managerUsername;
}