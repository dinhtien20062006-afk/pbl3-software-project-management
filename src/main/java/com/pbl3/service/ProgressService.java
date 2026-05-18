package com.pbl3.service;

import com.pbl3.dto.response.*;
import com.pbl3.entity.*;
import com.pbl3.exception.AppException;
import com.pbl3.exception.ErrorCode;
import com.pbl3.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProgressService {

    private final ProjectRepository projectRepository;
    private final ProjectTeamRepository projectTeamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    /**
     * LẤY TIẾN ĐỘ NHÓM - Dành cho Team Leader
     */
    @Transactional(readOnly = true)
    public TeamProgressResponse getTeamProgress(Long teamId) {
        User currentUser = getCurrentUser();
        ProjectTeam team = projectTeamRepository.findById(teamId)
                .orElseThrow(() -> new AppException(ErrorCode.TEAM_NOT_EXISTED));

        // PHÂN QUYỀN: Phải là Leader của nhóm, hoặc Manager của Dự án chứa nhóm đó, hoặc ADMIN
        boolean isLeader = team.getLeader().getId().equals(currentUser.getId());
        boolean isManager = team.getProject().getManager().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == User.Role.ADMIN;

        if (!isLeader && !isManager && !isAdmin) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        // 1. Tính toán thông tin chung phía trên
        long totalMembers = teamMemberRepository.countByProjectTeamId(teamId);
        long totalTasks = taskRepository.countByProjectTeamId(teamId);
        long doneTasks = taskRepository.countByProjectTeamIdAndStatus(teamId, Task.TaskStatus.DONE);
        
        double overallCompletionRate = totalTasks == 0 ? 0.0 : ((double) doneTasks / totalTasks) * 100;

        // 2. Tính toán danh sách tiến độ từng thành viên dưới quyền
        List<TeamMember> members = teamMemberRepository.findByProjectTeamId(teamId);
        List<TeamProgressResponse.MemberProgressItem> memberProgressList = new ArrayList<>();

        for (TeamMember member : members) {
            User user = member.getUser();
            
            long userTasks = taskRepository.countByProjectTeamIdAndAssigneeId(teamId, user.getId());
            long userDoneTasks = taskRepository.countByProjectTeamIdAndAssigneeIdAndStatus(teamId, user.getId(), Task.TaskStatus.DONE);
            long overdueTasks = taskRepository.countOverdueTasksByTeamAndUser(teamId, user.getId());

            double completionRate = userTasks == 0 ? 0.0 : ((double) userDoneTasks / userTasks) * 100;

            memberProgressList.add(TeamProgressResponse.MemberProgressItem.builder()
                    .username(user.getUsername())
                    .fullName(user.getFullName())
                    .assignedTasks(userTasks)
                    .overdueTasks(overdueTasks)
                    .completionRate(Math.round(completionRate * 10.0) / 10.0) // Làm tròn 1 chữ số thập phân
                    .progressDisplay(userDoneTasks + "/" + userTasks)
                    .build());
        }

        return TeamProgressResponse.builder()
                .teamName(team.getTeamName())
                .totalMembers(totalMembers)
                .totalTasks(totalTasks)
                .overallCompletionRate(Math.round(overallCompletionRate * 10.0) / 10.0)
                .memberProgresses(memberProgressList)
                .build();
    }

    /**
     * LẤY TIẾN ĐỘ DỰ ÁN - Dành cho Project Manager
     */
    @Transactional(readOnly = true)
    public ProjectProgressResponse getProjectProgress(Long projectId) {
        User currentUser = getCurrentUser();
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new AppException(ErrorCode.PROJECT_NOT_EXISTED));

        // PHÂN QUYỀN: Phải là Manager trực tiếp của dự án đó hoặc ADMIN hệ thống
        boolean isManager = project.getManager().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == User.Role.ADMIN;

        if (!isManager && !isAdmin) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        // 1. Tính toán thông tin chung dự án phía trên
        long totalMembers = teamMemberRepository.countUniqueMembersByProjectId(projectId);
        long totalTasks = taskRepository.countByProjectId(projectId);
        long doneTasks = taskRepository.countByProjectIdAndStatus(projectId, Task.TaskStatus.DONE);

        double overallCompletionRate = totalTasks == 0 ? 0.0 : ((double) doneTasks / totalTasks) * 100;

        // 2. Tính toán danh sách tiến độ của từng nhóm trong dự án phía dưới
        List<ProjectTeam> teams = projectTeamRepository.findByProjectId(projectId);
        List<ProjectProgressResponse.TeamProgressItem> teamProgressList = new ArrayList<>();

        for (ProjectTeam team : teams) {
            long teamMembersCount = teamMemberRepository.countByProjectTeamId(team.getId());
            long teamTasksCount = taskRepository.countByProjectTeamId(team.getId());
            long teamDoneTasksCount = taskRepository.countByProjectTeamIdAndStatus(team.getId(), Task.TaskStatus.DONE);

            double teamCompletionRate = teamTasksCount == 0 ? 0.0 : ((double) teamDoneTasksCount / teamTasksCount) * 100;

            teamProgressList.add(ProjectProgressResponse.TeamProgressItem.builder()
                    .teamId(team.getId())
                    .teamName(team.getTeamName())
                    .leaderName(team.getLeader() != null ? team.getLeader().getFullName() : "Chưa chỉ định")
                    .totalMembers(teamMembersCount)
                    .completionRate(Math.round(teamCompletionRate * 10.0) / 10.0)
                    .build());
        }

        return ProjectProgressResponse.builder()
                .projectName(project.getProjectName())
                .totalMembers(totalMembers)
                .totalTasks(totalTasks)
                .overallCompletionRate(Math.round(overallCompletionRate * 10.0) / 10.0)
                .teamProgresses(teamProgressList)
                .build();
    }
}