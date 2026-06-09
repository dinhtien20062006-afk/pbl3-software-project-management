package com.pbl3.service;

import com.pbl3.dto.request.*;
import com.pbl3.dto.response.TeamResponse;
import com.pbl3.entity.*;
import com.pbl3.exception.AppException;
import com.pbl3.exception.ErrorCode;
import com.pbl3.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ProjectTeamService {

    private final ProjectTeamRepository projectTeamRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final AuditLogService auditLogService;
    private final TaskRepository taskRepository;

    // Helper: Lấy User hiện tại
    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    // Tạo Team mới trong một dự án (Chỉ PM của dự án mới được tạo)
    @Transactional
    public TeamResponse createTeam(TeamRequest request) {
        User currentUser = getCurrentUser();
        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new AppException(ErrorCode.PROJECT_NOT_EXISTED));

        // Quyền: Chỉ Project Manager mới được tạo Team trong Project đó
        if (!isManager(project, currentUser)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        validateProjectIsEditable(project);

        User leader = userRepository.findById(request.getLeaderId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // 1. Lưu thông tin Team
        ProjectTeam team = ProjectTeam.builder()
                .teamName(request.getTeamName())
                .deadline(request.getDeadline())
                .description(request.getDescription())
                .project(project)
                .leader(leader)
                .status(ProjectTeam.TeamStatus.PLANNING)
                .build();

        ProjectTeam savedTeam = projectTeamRepository.save(team);

        // 2. Tự động thêm Leader vào danh sách TeamMember với vai trò LEADER
        TeamMember leaderMember = TeamMember.builder()
                .projectTeam(savedTeam)
                .user(leader)
                .memberRole("LEADER")
                .joinedAt(LocalDateTime.now())
                .build();
        teamMemberRepository.save(leaderMember);

        auditLogService.log(project, currentUser, AuditLog.AuditActionType.CREATE_TEAM,  team.getTeamName());
        
        return mapToResponse(savedTeam);
    }

    // Lấy danh sách Team của một dự án.
    // Trước khi hiển thị, tự cập nhật trạng thái nhóm theo ngày bắt đầu dự án
    // và tự chuyển nhóm sang COMPLETED nếu toàn bộ task trong nhóm đã DONE.
    @Transactional
    public List<TeamResponse> getTeamsByProject(Long projectId) {
        autoStartTeamsByProjectStartTime();
        autoCompleteTeamsWhenAllTasksDone();

        return projectTeamRepository.findByProjectId(projectId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // Lay tat ca Team ma User co lien quan: PM cua project, Leader cua team, hoac Member trong team.
    @Transactional(readOnly = true)
    public List<TeamResponse> getAllTeams() {
        User currentUser = getCurrentUser();

        if (currentUser.getRole() == User.Role.ADMIN) {
            return projectTeamRepository.findAll().stream()
                    .map(this::mapToResponse)
                    .toList();
        }

        return projectTeamRepository.findTeamsRelatedToUser(currentUser.getId()).stream()
                .map(this::mapToResponse)
                .toList();
    }

    private boolean isUserRelatedToTeam(ProjectTeam team, User user) {
        // Là Manager của dự án chứa Team này
        if (team.getProject().getManager().getId().equals(user.getId())) {
            return true;
        }
        
        // Là Leader của Team này
        if (team.getLeader().getId().equals(user.getId())) {
            return true;
        }

        // Là thành viên trong Team này
        return teamMemberRepository.existsByProjectTeamIdAndUserId(team.getId(), user.getId());
    }

    // Cập nhật thông tin Team (Chỉ PM của dự án lớn mới được cập nhật)
    @Transactional
    public TeamResponse updateTeam(Long teamId, TeamRequest request) {
        User currentUser = getCurrentUser();
        ProjectTeam team = projectTeamRepository.findById(teamId)
                .orElseThrow(() -> new AppException(ErrorCode.TEAM_NOT_EXISTED));

        // Quyền: Chỉ PM của dự án lớn mới được đổi thông tin Team
        if (!isManager(team.getProject(), currentUser)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        validateProjectIsEditable(team.getProject());

        if (request.getTeamName() != null) team.setTeamName(request.getTeamName());
        if (request.getDeadline() != null) team.setDeadline(request.getDeadline());
        if (request.getDescription() != null) team.setDescription(request.getDescription());

        // Nếu thay đổi Leader
        if (request.getLeaderId() != null && !team.getLeader().getId().equals(request.getLeaderId())) {
            User newLeader = userRepository.findById(request.getLeaderId())
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
            team.setLeader(newLeader);

            // Đảm bảo Leader mới cũng nằm trong danh sách TeamMember
            if (!teamMemberRepository.existsByProjectTeamIdAndUserId(team.getId(), newLeader.getId())) {
                TeamMember leaderMember = TeamMember.builder()
                        .projectTeam(team)
                        .user(newLeader)
                        .memberRole("LEADER")
                        .joinedAt(LocalDateTime.now())
                        .build();
                teamMemberRepository.save(leaderMember);
            }
        }

        ProjectTeam updatedTeam = projectTeamRepository.save(team);
        auditLogService.log(team.getProject(), currentUser, AuditLog.AuditActionType.UPDATE_TEAM, team.getTeamName());

        return mapToResponse(updatedTeam);
    }

    // Xóa Team (Chỉ PM của dự án mới được xóa)
    @Transactional
    public void deleteTeam(Long teamId) {
        User currentUser = getCurrentUser();
        ProjectTeam team = projectTeamRepository.findById(teamId)
                .orElseThrow(() -> new AppException(ErrorCode.TEAM_NOT_EXISTED));

        if (!isManager(team.getProject(), currentUser)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        validateProjectIsEditable(team.getProject());
        
        auditLogService.log(team.getProject(), currentUser, AuditLog.AuditActionType.DELETE_TEAM, team.getTeamName());

        projectTeamRepository.delete(team);
    }

    // Bắt đầu nhóm (PM hoặc Leader mới được bắt đầu nhóm)
    @Transactional
    public TeamResponse startTeam(Long teamId) {
        User currentUser = getCurrentUser();
        ProjectTeam team = projectTeamRepository.findById(teamId)
                .orElseThrow(() -> new AppException(ErrorCode.TEAM_NOT_EXISTED));

        // Quyền: Chỉ PM của dự án hoặc Leader của Team mới được bắt đầu nhóm
        if (!isManager(team.getProject(), currentUser) &&
            !isLeader(team, currentUser)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        validateProjectIsEditable(team.getProject());

        team.setStatus(ProjectTeam.TeamStatus.IN_PROGRESS);
        ProjectTeam updatedTeam = projectTeamRepository.save(team);
        auditLogService.log(team.getProject(), currentUser, AuditLog.AuditActionType.START_TEAM, team.getTeamName());

        return mapToResponse(updatedTeam);
    }


    // Đánh dấu nhóm hoàn thành khi tất cả task trong nhóm đã DONE
    @Transactional
    public TeamResponse completeTeam(Long teamId) {
        User currentUser = getCurrentUser();
        ProjectTeam team = projectTeamRepository.findById(teamId)
                .orElseThrow(() -> new AppException(ErrorCode.TEAM_NOT_EXISTED));

        if (!isManager(team.getProject(), currentUser) && !isLeader(team, currentUser)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        validateProjectIsEditable(team.getProject());

        validateAllTasksDone(teamId);

        team.setStatus(ProjectTeam.TeamStatus.COMPLETED);
        ProjectTeam updatedTeam = projectTeamRepository.save(team);
        auditLogService.log(team.getProject(), currentUser, AuditLog.AuditActionType.COMPLETE_TEAM, team.getTeamName());
        return mapToResponse(updatedTeam);
    }

    // Tự chuyển nhóm PLANNING sang IN_PROGRESS khi đã đến thời gian bắt đầu dự án.
    // Không tự chạy với dự án đang ON_HOLD.
    @Transactional
    public void autoStartTeamsByProjectStartTime() {
        LocalDateTime now = LocalDateTime.now();
        List<ProjectTeam> teams = projectTeamRepository.findTeamsNeedAutoStart(
                ProjectTeam.TeamStatus.PLANNING,
                Project.ProjectStatus.IN_PROGRESS,
                now
        );

        if (teams.isEmpty()) {
            return;
        }

        teams.forEach(team -> team.setStatus(ProjectTeam.TeamStatus.IN_PROGRESS));
        projectTeamRepository.saveAll(teams);
    }

    // Tự chuyển nhóm IN_PROGRESS sang COMPLETED nếu nhóm có task và tất cả task đã DONE.
    @Transactional
    public void autoCompleteTeamsWhenAllTasksDone() {
        List<ProjectTeam> teams = projectTeamRepository.findByStatus(ProjectTeam.TeamStatus.IN_PROGRESS);

        for (ProjectTeam team : teams) {
            long totalTasks = taskRepository.countByProjectTeamId(team.getId());
            if (totalTasks == 0) {
                continue;
            }

            long doneTasks = taskRepository.countByProjectTeamIdAndStatus(team.getId(), Task.TaskStatus.DONE);
            if (totalTasks == doneTasks) {
                team.setStatus(ProjectTeam.TeamStatus.COMPLETED);
                projectTeamRepository.save(team);
            }
        }
    }

    private void validateAllTasksDone(Long teamId) {
        long totalTasks = taskRepository.countByProjectTeamId(teamId);
        if (totalTasks == 0) {
            throw new AppException(ErrorCode.TEAM_NOT_READY_TO_COMPLETE);
        }

        long doneTasks = taskRepository.countByProjectTeamIdAndStatus(teamId, Task.TaskStatus.DONE);
        if (totalTasks != doneTasks) {
            throw new AppException(ErrorCode.TEAM_NOT_READY_TO_COMPLETE);
        }
    }

    // Leader gửi yêu cầu đổi deadline của nhóm cho Project Manager duyệt
    @Transactional
    public TeamResponse requestDeadlineChange(Long teamId, String reason, LocalDateTime requestedDeadline) {
        User currentUser = getCurrentUser();
        ProjectTeam team = projectTeamRepository.findById(teamId)
                .orElseThrow(() -> new AppException(ErrorCode.TEAM_NOT_EXISTED));

        if (!isLeader(team, currentUser)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        validateProjectIsEditable(team.getProject());

        if (requestedDeadline == null) {
            throw new AppException(ErrorCode.INVALID_DEADLINE);
        }

        Project project = team.getProject();
        if (project.getStartDate() != null && requestedDeadline.isBefore(project.getStartDate())) {
            throw new AppException(ErrorCode.INVALID_DEADLINE);
        }
        if (project.getEndDate() != null && requestedDeadline.isAfter(project.getEndDate())) {
            throw new AppException(ErrorCode.INVALID_DEADLINE);
        }

        team.setRequestedDeadline(requestedDeadline);
        team.setDeadlineRequestReason(reason);

        ProjectTeam updatedTeam = projectTeamRepository.save(team);
        auditLogService.log(project, currentUser, AuditLog.AuditActionType.EXTEND_DEADLINE, team.getTeamName());
        return mapToResponse(updatedTeam);
    }

    // Project Manager chấp nhận hoặc từ chối yêu cầu đổi deadline của Leader
    @Transactional
    public TeamResponse resolveDeadlineChangeRequest(Long teamId, boolean approved) {
        User currentUser = getCurrentUser();
        ProjectTeam team = projectTeamRepository.findById(teamId)
                .orElseThrow(() -> new AppException(ErrorCode.TEAM_NOT_EXISTED));

        if (!isManager(team.getProject(), currentUser)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        validateProjectIsEditable(team.getProject());

        if (team.getRequestedDeadline() == null) {
            throw new AppException(ErrorCode.INVALID_ASSIGNMENT);
        }

        if (approved) {
            LocalDateTime requestedDeadline = team.getRequestedDeadline();
            Project project = team.getProject();
            if (project.getStartDate() != null && requestedDeadline.isBefore(project.getStartDate())) {
                throw new AppException(ErrorCode.INVALID_DEADLINE);
            }
            if (project.getEndDate() != null && requestedDeadline.isAfter(project.getEndDate())) {
                throw new AppException(ErrorCode.INVALID_DEADLINE);
            }
            team.setDeadline(requestedDeadline);
        }

        team.setRequestedDeadline(null);
        team.setDeadlineRequestReason(null);

        ProjectTeam updatedTeam = projectTeamRepository.save(team);
        auditLogService.log(team.getProject(), currentUser, AuditLog.AuditActionType.UPDATE_TEAM, team.getTeamName());
        return mapToResponse(updatedTeam);
    }


    private void validateProjectIsEditable(Project project) {
        if (project.getStatus() == Project.ProjectStatus.ON_HOLD
                || project.getStatus() == Project.ProjectStatus.COMPLETED) {
            throw new AppException(ErrorCode.INVALID_ASSIGNMENT);
        }
    }

    //Helper check quyền
    boolean isManager(Project project, User user) {
        return project.getManager().getId().equals(user.getId());
    }
    boolean isLeader(ProjectTeam team, User user) {
        return team.getLeader().getId().equals(user.getId());
    }

    private TeamResponse mapToResponse(ProjectTeam team) {        
        return TeamResponse.builder()
                .projectId(team.getProject().getId())
                .teamId(team.getId())
                .teamName(team.getTeamName())
                .description(team.getDescription())
                .deadline(team.getDeadline())
                .status(team.getStatus())
                .requestedDeadline(team.getRequestedDeadline())
                .deadlineRequestReason(team.getDeadlineRequestReason())
                .leaderName(team.getLeader().getFullName())
                .leaderUsername(team.getLeader().getUsername())
                .leaderId(team.getLeader().getId())
                .managerName(team.getProject().getManager().getFullName())
                .managerUsername(team.getProject().getManager().getUsername())
                .build();
    }
}