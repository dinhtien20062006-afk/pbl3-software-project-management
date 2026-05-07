package com.pbl3.service;

import com.pbl3.dto.request.TeamCreateRequest;
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

@Service
@RequiredArgsConstructor
public class ProjectTeamService {

    private final ProjectTeamRepository projectTeamRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final AuditLogService auditLogService;

    // Lấy User đang đăng nhập hiện tại
    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    // Manager tạo nhóm nhỏ cho dự án của mình, đồng thời gán các thành viên đã chọn vào nhóm đó
    @Transactional
    public TeamResponse createTeam(TeamCreateRequest request) {
        User currentUser = getCurrentUser();
        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new AppException(ErrorCode.PROJECT_NOT_EXISTED));

        // 1. Chỉ Manager của dự án mới được tạo nhóm nhỏ
        if (!project.getManager().getId().equals(currentUser.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        // 2. Kiểm tra Leader có tồn tại không
        User leader = userRepository.findById(request.getLeaderId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // 3. Tạo thực thể Team
        ProjectTeam team = ProjectTeam.builder()
                .teamName(request.getTeamName())
                .description(request.getDescription())
                .deadline(request.getDeadline())
                .project(project)
                .leader(leader)
                .build();

        ProjectTeam savedTeam = projectTeamRepository.save(team);

        // 4. Gán các thành viên đã chọn vào nhóm này
        if (request.getMemberIds() != null && !request.getMemberIds().isEmpty()) {
            List<ProjectMember> members = projectMemberRepository.findAllById(request.getMemberIds());
            for (ProjectMember member : members) {
                // Đảm bảo thành viên này thuộc đúng dự án đó
                if (member.getProject().getId().equals(project.getId())) {
                    member.setProjectTeam(savedTeam);
                    // Có thể cập nhật Role thành LEADER cho người trưởng nhóm trong bảng ProjectMember
                    if (member.getUser().getId().equals(leader.getId())) {
                        member.setProjectRole("LEADER");
                    }
                }
            }
            projectMemberRepository.saveAll(members);
        }

        auditLogService.log(project, currentUser, AuditLog.AuditActionType.UPDATE_PROJECT, "Tạo nhóm: " + team.getTeamName());
        
        return mapToResponse(savedTeam);
    }

    // Lấy danh sách tất cả nhóm theo dự án dành cho Manager
    public List<TeamResponse> getTeamsByProject(Long projectId) {
        return projectTeamRepository.findByProjectId(projectId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // Lấy danh sách nhóm mà Leader đang quản lý
    public List<TeamResponse> getMyManagedTeams() {
        User currentUser = getCurrentUser();
        return projectTeamRepository.findByLeaderId(currentUser.getId()).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // Lấy thông tin chi tiết của một nhóm mà mình tham gia
    public TeamResponse getTeamDetails(Long teamId) {
        User currentUser = getCurrentUser();
        ProjectTeam team = projectTeamRepository.findById(teamId)
                .orElseThrow(() -> new AppException(ErrorCode.TEAM_NOT_EXISTED));

        // Kiểm tra xem user có phải là leader hoặc thành viên của nhóm không
        boolean isMember = team.getMembers() != null && team.getMembers().stream()
                .anyMatch(m -> m.getUser().getId().equals(currentUser.getId()));
        if (!team.getLeader().getId().equals(currentUser.getId()) && !isMember) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        return mapToResponse(team);
    }

    private TeamResponse mapToResponse(ProjectTeam team) {
        return TeamResponse.builder()
                .teamId(team.getId())
                .teamName(team.getTeamName())
                .description(team.getDescription())
                .deadline(team.getDeadline())
                .leaderName(team.getLeader().getFullName())
                .memberNames(team.getMembers() != null ? 
                        team.getMembers().stream().map(m -> m.getUser().getFullName()).toList() : List.of())
                .build();
    }
}