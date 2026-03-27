package com.pbl3.dto.request;

import lombok.Data;
import java.time.LocalDate;

@Data 
public class ProjectRequest {

    // Dữ liệu client gửi lên

    private String projectName;

    private String description;

    private LocalDate startDate;

    private LocalDate endDate;

    //  Không có id
    //  Không có status
}