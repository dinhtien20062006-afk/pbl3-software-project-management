package com.pbl3.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AutoStatusService {

    private final ProjectService projectService;
    private final ProjectTeamService projectTeamService;

    // Chạy định kỳ 1 phút/lần để tự cập nhật trạng thái dự án và nhóm.
    // - Đến giờ bắt đầu dự án: Project -> IN_PROGRESS, Team PLANNING -> IN_PROGRESS.
    // - Khi toàn bộ task DONE: Team/Project -> COMPLETED.
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void refreshStatusesByTimeAndProgress() {
        projectService.refreshProjectStatuses();
        projectTeamService.autoStartTeamsByProjectStartTime();
        projectTeamService.autoCompleteTeamsWhenAllTasksDone();
    }
}
