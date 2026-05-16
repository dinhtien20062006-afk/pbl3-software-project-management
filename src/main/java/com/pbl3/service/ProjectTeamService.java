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
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class ProjectTeamService {

    private final ProjectTeamRepository projectTeamRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final AuditLogService auditLogService;

    // Helper: Lấy User hiện tại
    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    // Tạo Team mới trong một dự án (Chỉ PM của dự án mới được tạo)
    @Transactional
    public TeamResponse createTeam(TeamCreateRequest request) {
        User currentUser = getCurrentUser();
        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new AppException(ErrorCode.PROJECT_NOT_EXISTED));

        // Quyền: Chỉ Project Manager mới được tạo Team trong Project đó
        if (!isManager(project, currentUser)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

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
                .joinedAt(LocalDate.now())
                .build();
        teamMemberRepository.save(leaderMember);

        auditLogService.log(project, currentUser, AuditLog.AuditActionType.CREATE_TEAM,  team.getTeamName());
        
        return mapToResponse(savedTeam);
    }

    // Lấy danh sách Team của một dự án 
    @Transactional(readOnly = true)
    public List<TeamResponse> getTeamsByProject(Long projectId) {
        return projectTeamRepository.findByProjectId(projectId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // Lấy tất cả Team mà User có liên quan (Là PM của dự án chứa Team đó, hoặc là Leader/Member của Team đó)
    @Transactional(readOnly = true)
    public List<TeamResponse> getAllTeams() {
        User currentUser = getCurrentUser();

        //Nếu là PROJECT_MANAGER: Thấy nhóm của dự án mình quản lý + nhóm mình tham gia
        if (currentUser.getRole() == User.Role.PROJECT_MANAGER) {
            // Lấy tất cả các team mà user này là PM của dự án đó HOẶC là leader/member của team đó
            return projectTeamRepository.findAll().stream()
                    .filter(team -> isUserRelatedToTeam(team, currentUser))
                    .map(this::mapToResponse)
                    .toList();
        }

        // Nếu là MEMBER: Chỉ thấy những nhóm mình thực sự tham gia (là Leader hoặc là Thành viên)
        List<Long> joinedTeamIds = teamMemberRepository.findAllTeamIdsByUserId(currentUser.getId());
        return projectTeamRepository.findAllById(joinedTeamIds).stream()
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
    public TeamResponse updateTeam(Long teamId, TeamUpdateRequest request) {
        User currentUser = getCurrentUser();
        ProjectTeam team = projectTeamRepository.findById(teamId)
                .orElseThrow(() -> new AppException(ErrorCode.TEAM_NOT_EXISTED));

        // Quyền: Chỉ PM của dự án lớn mới được đổi thông tin Team
        if (!isManager(team.getProject(), currentUser)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        team.setTeamName(request.getTeamName());
        team.setDeadline(request.getDeadline());
        team.setDescription(request.getDescription());

        // Nếu thay đổi Leader
        if (!team.getLeader().getId().equals(request.getLeaderId())) {
            User newLeader = userRepository.findById(request.getLeaderId())
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
            team.setLeader(newLeader); 
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

        team.setStatus(ProjectTeam.TeamStatus.IN_PROGRESS);
        ProjectTeam updatedTeam = projectTeamRepository.save(team);
        auditLogService.log(team.getProject(), currentUser, AuditLog.AuditActionType.START_TEAM, team.getTeamName());

        return mapToResponse(updatedTeam);
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
                .teamId(team.getId())
                .teamName(team.getTeamName())
                .description(team.getDescription())
                .deadline(team.getDeadline())
                .status(team.getStatus())
                .leaderName(team.getLeader().getFullName())
                .managerName(team.getProject().getManager().getFullName())
                .build();
    }
}