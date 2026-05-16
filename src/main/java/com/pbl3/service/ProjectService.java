package com.pbl3.service;

import com.pbl3.dto.request.CreateProjectRequest;
import com.pbl3.dto.request.UpdateProjectRequest;
import com.pbl3.dto.response.ProjectResponse;
import com.pbl3.entity.*;
import com.pbl3.exception.AppException;
import com.pbl3.exception.ErrorCode;
import com.pbl3.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final AuditLogService auditLogService;

    // Lấy user hiện tại đang đăng nhập
    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    @Transactional
    public ProjectResponse createProject(CreateProjectRequest request) {
        User currentUser = getCurrentUser();

        // Chỉ PROJECT_MANAGER hoặc ADMIN mới có quyền tạo dự án
        if (currentUser.getRole() == User.Role.MEMBER) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new AppException(ErrorCode.PROJECT_TIME_INVALID);
        }

        Project project = Project.builder()
                .projectName(request.getProjectName())
                .description(request.getDescription())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(Project.ProjectStatus.PLANNING)
                .manager(currentUser) // Người tạo là Manager chính
                .build();

        Project savedProject = projectRepository.save(project);
        
        auditLogService.log(savedProject, currentUser, AuditLog.AuditActionType.CREATE_PROJECT, project.getProjectName());
        
        return mapToResponse(savedProject);
    }

    @Transactional
    public ProjectResponse updateProject(Long id, UpdateProjectRequest request) {
        User currentUser = getCurrentUser();
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PROJECT_NOT_EXISTED));

        // Kiểm tra quyền: Chỉ Manager của dự án hoặc ADMIN mới được sửa
        if (!project.getManager().getId().equals(currentUser.getId()) && currentUser.getRole() != User.Role.ADMIN) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        if (request.getProjectName() != null) project.setProjectName(request.getProjectName());
        if (request.getDescription() != null) project.setDescription(request.getDescription());
        if (request.getStartDate() != null) project.setStartDate(request.getStartDate());
        if (request.getEndDate() != null) project.setEndDate(request.getEndDate());
        if (request.getStatus() != null) project.setStatus(request.getStatus());

        if (project.getEndDate().isBefore(project.getStartDate())) {
            throw new AppException(ErrorCode.PROJECT_TIME_INVALID);
        }

        auditLogService.log(project, currentUser, AuditLog.AuditActionType.UPDATE_PROJECT, project.getProjectName());
        return mapToResponse(projectRepository.save(project));
    }

    @Transactional
    public void deleteProject(Long id) {
        User currentUser = getCurrentUser();
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PROJECT_NOT_EXISTED));

        if (!project.getManager().getId().equals(currentUser.getId()) && currentUser.getRole() != User.Role.ADMIN) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        auditLogService.log(project, currentUser, AuditLog.AuditActionType.DELETE_PROJECT, project.getProjectName());
        projectRepository.delete(project);
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> getAllProjects() {
        User currentUser = getCurrentUser();

        // Nếu là ADMIN: Thấy tất cả
        if (currentUser.getRole() == User.Role.ADMIN) {
            return projectRepository.findAll().stream().map(this::mapToResponse).toList();
        }

        // Nếu là User: Thấy dự án mình làm Manager HOẶC dự án có Nhóm mà mình là thành viên
        return projectRepository.findAll().stream()
                .filter(p -> isUserInProject(p, currentUser))
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProjectById(Long id) {
        User currentUser = getCurrentUser();
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PROJECT_NOT_EXISTED));

        if (currentUser.getRole() != User.Role.ADMIN && !isUserInProject(project, currentUser)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        return mapToResponse(project);
    }

    // Kiểm tra xem User có liên quan đến dự án (là Manager hoặc thành viên của bất kỳ Team nào trong dự án)
    private boolean isUserInProject(Project project, User user) {
    if (project.getManager().getId().equals(user.getId())) return true;
    return teamMemberRepository.existsByProjectTeam_ProjectIdAndUserId(project.getId(), user.getId());
    }

    // Tìm dự án theo tên (cho phép tìm kiếm một phần tên)
    @Transactional(readOnly = true)
    public List<ProjectResponse> getProjectsByName(String name) {
        User currentUser = getCurrentUser();
        return projectRepository.findByProjectNameContainingIgnoreCase(name).stream()
                .filter(p -> isUserInProject(p, currentUser))
                .map(this::mapToResponse)
                .toList();
    }

    private ProjectResponse mapToResponse(Project project) {
        return ProjectResponse.builder()
                .id(project.getId())
                .projectName(project.getProjectName())
                .description(project.getDescription())
                .startDate(project.getStartDate())
                .endDate(project.getEndDate())
                .status(project.getStatus())
                .managerName(project.getManager().getFullName())
                .managerUsername(project.getManager().getUsername())
                .build();
    }
}