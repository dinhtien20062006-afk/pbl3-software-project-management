package com.pbl3.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class DashboardResponse {
    private String role; // Vai trò hiện tại của người xem để Vaadin ẩn/hiện Component tương ứng
    
    // 1. Thẻ tóm tắt (Cards/Metrics)
    private Map<String, Long> metrics; 

    // 2. Dữ liệu tiến độ để vẽ biểu đồ hình tròn/cột (Chart Data)
    private Map<String, Long> taskStatusOverview; // Thống kê Task theo trạng thái (TODO, IN_PROGRESS, DONE,...)

    // 3. Danh sách công việc khẩn cấp/gần đây để bind vào Vaadin Grid
    private List<DashboardTaskItem> recentTasks;

    @Getter
    @Setter
    @Builder
    public static class DashboardTaskItem {
        private Long id;
        private String taskName;
        private String projectName;
        private String teamName;
        private String status;
        private String priority;
        private LocalDateTime deadline;
    }
}