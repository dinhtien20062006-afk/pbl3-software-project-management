package com.pbl3.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

import com.pbl3.entity.ProjectStatus;

@Data
@Builder // Cho phép build object
public class ProjectResponse {

    // Dữ liệu trả về client

    private Long id;

    private String projectName;

    private String description;

    private LocalDate startDate;

    private LocalDate endDate;

    private ProjectStatus status;
}