package com.pbl3.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
@Builder
public class TeamProgressResponse {
    // Phía trên: Thông tin chung của nhóm
    private String teamName;
    private long totalMembers;
    private long totalTasks;
    private double overallCompletionRate; // Tiến độ hoàn thành chung (e.g., 75.5%)

    // Phía dưới: Danh sách tiến độ từng thành viên
    private List<MemberProgressItem> memberProgresses;

    @Getter
    @Setter
    @Builder
    public static class MemberProgressItem {
        private String username;
        private String fullName;
        private long assignedTasks;
        private long overdueTasks;
        private double completionRate; // (Tổng task DONE / Tổng task được giao) * 100
        private String progressDisplay; // Định dạng chuỗi hiển thị ví dụ: "8/10 Tasks"
    }
}