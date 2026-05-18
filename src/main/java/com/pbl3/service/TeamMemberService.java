package com.pbl3.service;

import com.pbl3.dto.request.*;
import com.pbl3.dto.response.*;
import com.pbl3.entity.*;
import com.pbl3.exception.AppException;
import com.pbl3.exception.ErrorCode;
import com.pbl3.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeamMemberService {

    private final TeamMemberRepository teamMemberRepository;
    private final ProjectTeamRepository projectTeamRepository;
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final AuditLogService auditLogService;

    // Helper: Lấy User hiện tại
    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    // 1. Lấy danh sách thành viên của nhóm
    public List<TeamMemberResponse> getMembersByTeam(Long teamId) {
        if (!projectTeamRepository.existsById(teamId)) {
            throw new AppException(ErrorCode.TEAM_NOT_EXISTED);
        }

        return teamMemberRepository.findByProjectTeamId(teamId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // 2. Thêm thành viên vào nhóm
    @Transactional
    public void addMemberToTeam(Long teamId, TeamMemberRequest request) {
        ProjectTeam team = projectTeamRepository.findById(teamId)
                .orElseThrow(() -> new AppException(ErrorCode.TEAM_NOT_EXISTED));

        User currentUser = getCurrentUser();
        
        // Kiểm tra quyền: PM của dự án HOẶC Leader của nhóm
        boolean isProjectManager = isManager(team.getProject(), currentUser);
        boolean isTeamLeader = isTeamLeader(team, currentUser);

        if (!isProjectManager && !isTeamLeader) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        User userToAdd = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // Kiểm tra nếu user đã có trong nhóm
        if (teamMemberRepository.existsByProjectTeamIdAndUserId(teamId, request.getUserId())) {
            throw new AppException(ErrorCode.MEMBER_ALREADY_EXISTED);
        }

        TeamMember teamMember = TeamMember.builder()
                .projectTeam(team)
                .user(userToAdd)
                .memberRole(request.getRole()) // e.g., DEVELOPER, TESTER
                .joinedAt(LocalDateTime.now())
                .build();

        teamMemberRepository.save(teamMember);
        
        auditLogService.log(team.getProject(), currentUser, AuditLog.AuditActionType.ADD_MEMBER, userToAdd.getFullName());
    }

    // 3. Xóa thành viên khỏi nhóm
    @Transactional
    public void removeMemberFromTeam(Long teamId, Long userId) {
        ProjectTeam team = projectTeamRepository.findById(teamId)
                .orElseThrow(() -> new AppException(ErrorCode.TEAM_NOT_EXISTED));

        User currentUser = getCurrentUser();
        
        // Không cho phép tự xóa Leader của nhóm qua hàm này (phải update team)
        if (team.getLeader().getId().equals(userId)) {
            throw new AppException(ErrorCode.CANNOT_REMOVE_LEADER); 
        }

        // Kiểm tra quyền: PM của dự án HOẶC Leader của nhóm
        boolean isProjectManager = isManager(team.getProject(), currentUser);
        boolean isTeamLeader = isTeamLeader(team, currentUser);

        if (!isProjectManager && !isTeamLeader) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        TeamMember member = teamMemberRepository.findByProjectTeamIdAndUserId(teamId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.MEMBER_NOT_EXISTED));

        // XỬ LÝ NHIỆM VỤ: Gỡ Assignee khỏi các task của User này trong TEAM này
        List<Task> assignedTasks = taskRepository.findByProjectTeamIdAndAssigneeId(teamId, userId);
        assignedTasks.forEach(task -> task.setAssignee(null));
        taskRepository.saveAll(assignedTasks);

        teamMemberRepository.delete(member);

        auditLogService.log(team.getProject(), currentUser, AuditLog.AuditActionType.REMOVE_MEMBER, member.getUser().getFullName());
    }

    // 4. Thành viên tự rời nhóm
    @Transactional
    public void leaveTeam(Long teamId) {
        User currentUser = getCurrentUser();
        
        TeamMember member = teamMemberRepository.findByProjectTeamIdAndUserId(teamId, currentUser.getId())
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED));

        ProjectTeam team = member.getProjectTeam();

        // Leader không được tự ý rời nhóm
        if (isTeamLeader(team, currentUser)) {
            throw new AppException(ErrorCode.LEADER_CANNOT_LEAVE);
        }

        // Gỡ Assignee các task liên quan
        List<Task> assignedTasks = taskRepository.findByProjectTeamIdAndAssigneeId(teamId, currentUser.getId());
        assignedTasks.forEach(task -> task.setAssignee(null));
        taskRepository.saveAll(assignedTasks);

        teamMemberRepository.delete(member);

        auditLogService.log(team.getProject(), currentUser, AuditLog.AuditActionType.LEAVE_TEAM, team.getTeamName());
    }

    boolean isManager(Project project, User user) {
        return project.getManager().getId().equals(user.getId());
    }
    boolean isTeamLeader(ProjectTeam team, User user) {
        return team.getLeader().getId().equals(user.getId());
    }

    private TeamMemberResponse mapToResponse(TeamMember tm) {
        return TeamMemberResponse.builder()
                .userId(tm.getUser().getId())
                .fullName(tm.getUser().getFullName())
                .username(tm.getUser().getUsername())
                .role(tm.getMemberRole())
                .joinedAt(tm.getJoinedAt())
                .build();
    }
}