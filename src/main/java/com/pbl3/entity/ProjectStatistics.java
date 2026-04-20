package com.pbl3.entity;

import lombok.Builder;
import lombok.Data;
@Data
@Builder
public class ProjectStatistics {
    private Long totalTasks;
    private Long completedTasks;
    private Long overDeadlineTasks;
    private Double completionPercentage; // Ví dụ: 75.5%
}
