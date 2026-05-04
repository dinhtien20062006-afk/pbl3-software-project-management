package com.pbl3.dto.request;

import lombok.Data;
import java.time.LocalDate;

import com.pbl3.entity.Project;

@Data 
public class UpdateProjectRequest {
    private String projectName;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private Project.ProjectStatus status; 
    
}