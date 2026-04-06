package com.pbl3.dto.request;

import lombok.Data;
import java.time.LocalDate;

import com.pbl3.entity.ProjectStatus;

@Data 
public class UpdateProjectRequest {

    // Dữ liệu client gửi lên

    private String projectName;

    private String description;

    private LocalDate startDate;

    private LocalDate endDate;

    private ProjectStatus status; 
    
}