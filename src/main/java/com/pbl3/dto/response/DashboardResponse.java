package com.pbl3.dto.response;

import lombok.*;
import java.util.Map;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse {
    // 1. Thẻ con số (Cards/Widgets)
    private Statistics stats;

    // 2. Dữ liệu biểu đồ (Charts)
    private Map<String, Long> taskDistribution;    // Biểu đồ tròn: Trạng thái công việc
    private Map<String, Long> projectDistribution; // Biểu đồ tròn: Trạng thái dự án (Dành cho Admin)

    // 3. Danh sách hiển thị (Grids/Tables)
    private List<AuditLogResponse> recentActivities; // Nhật ký hoạt động gần đây
    private List<ProjectResponse> activeProjects;     // Danh sách dự án đang thực hiện

    @Data
    @Builder
    public static class Statistics {
        private long totalUsers;
        private long totalProjects;
        private long totalTasks;
        private long pendingTasks; // Task đang chờ duyệt (quan trọng cho Manager)
        private long completedTasks;
    }
}