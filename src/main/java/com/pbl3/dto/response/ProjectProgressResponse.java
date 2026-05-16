package com.pbl3.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
@Builder
public class ProjectProgressResponse {
    // Phía trên: Thông tin chung của dự án
    private String projectName;
    private long totalMembers; // Tổng số nhân sự độc nhất tham gia dự án
    private long totalTasks;
    private double overallCompletionRate; // Tiến độ hoàn thành chung của dự án

    // Phía dưới: Danh sách tiến độ từng nhóm
    private List<TeamProgressItem> teamProgresses;

    @Getter
    @Setter
    @Builder
    public static class TeamProgressItem {
        private Long teamId;
        private String teamName;
        private String leaderName;
        private long totalMembers;
        private double completionRate; // Tiến độ hoàn thành riêng của nhóm đó
    }
}