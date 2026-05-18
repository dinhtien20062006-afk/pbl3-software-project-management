package com.pbl3.dto.request;

import lombok.Data;
import java.time.LocalDateTime;

import com.pbl3.entity.Project;

@Data 
public class ProjectRequest {
    private String projectName;
    private String description;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Project.ProjectStatus status;
}