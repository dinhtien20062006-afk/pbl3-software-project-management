package com.pbl3.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;

import com.pbl3.entity.ProjectStatus;

@Data
@AllArgsConstructor
public class ProjectResponse {

    // Dữ liệu trả về client

    private Long id;

    private String projectName;

    private String description;

    private LocalDate startDate;

    private LocalDate endDate;

    private ProjectStatus status;
}