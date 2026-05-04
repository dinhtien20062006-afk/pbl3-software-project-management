package com.pbl3.dto.request;

import lombok.Data;
import java.time.LocalDate;

@Data 
public class CreateProjectRequest {
    private String projectName;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
}