package com.pbl3.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
public class DashboardResponse {
    private String role; // Vai trò hiện tại của người xem để Vaadin ẩn/hiện Component tương ứng
    
    // 1. Thẻ tóm tắt (Cards/Metrics)
    private Map<String, Long> metrics; 
    /* 
       ADMIN: {"totalUsers": 50, "totalProjects": 12, "totalTeams": 24, "doneTasks": 120}
       PM:    {"myProjects": 3, "myTeams": 8, "totalMembers": 15, "pendingApprovalTasks": 5}
       MEMBER:{"myTodoTasks": 4, "myInProgressTasks": 2, "myDoneTasks": 10, "overdueTasks": 1}
    */

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
        private String deadline;
    }
}