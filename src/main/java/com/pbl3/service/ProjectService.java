package com.pbl3.service;

import com.pbl3.dto.request.CreateProjectRequest;
import com.pbl3.dto.request.UpdateProjectRequest;
import com.pbl3.dto.response.ProjectResponse;
import com.pbl3.entity.*;
import com.pbl3.exception.AppException;
import com.pbl3.exception.ErrorCode;
import com.pbl3.repository.ProjectRepository;
import com.pbl3.repository.UserRepository;
import com.pbl3.repository.ProjectMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final AuditLogService auditLogService;

    // Lấy user hiện tại đang đăng nhập từ SecurityContext
    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    @Transactional
    public ProjectResponse createProject(CreateProjectRequest request) {
        User currentUser = getCurrentUser();

        // Chỉ Project Manager mới có quyền tạo dự án 
        if (!(currentUser.getRole() == User.Role.PROJECT_MANAGER)) {
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
                .manager(currentUser)
                .build();

        Project savedProject = projectRepository.save(project);

        // Tự động thêm Manager vào bảng ProjectMember
        ProjectMember managerMember = ProjectMember.builder()
                .project(savedProject)
                .user(currentUser)
                .projectRole("PROJECT_MANAGER") 
                .joinedAt(LocalDate.now())
                .build();
        
        projectMemberRepository.save(managerMember);
        auditLogService.log(savedProject, currentUser, AuditLog.AuditActionType.CREATE_PROJECT, project.getProjectName());
        return mapToResponse(savedProject);
    }

    @Transactional
    public ProjectResponse updateProject(Long id, UpdateProjectRequest request) {
        User currentUser = getCurrentUser();
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PROJECT_NOT_EXISTED));

        // Kiểm tra quyền: Chỉ chủ dự án (Manager) mới được sửa
        if (!project.getManager().getId().equals(currentUser.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        if (request.getProjectName() != null) project.setProjectName(request.getProjectName());
        if (request.getDescription() != null) project.setDescription(request.getDescription());
        if (request.getStartDate() != null) project.setStartDate(request.getStartDate());
        if (request.getEndDate() != null) project.setEndDate(request.getEndDate());
        if (request.getStatus() != null) project.setStatus(request.getStatus());

        // Kiểm tra logic ngày tháng sau khi cập nhật 
        if (request.getEndDate() != null && request.getEndDate().isBefore(request.getStartDate())) {
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

        if (!project.getManager().getId().equals(currentUser.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        // Xóa tất cả thành viên của dự án này trước
        projectMemberRepository.deleteByProjectId(id);

        auditLogService.log(project, currentUser, AuditLog.AuditActionType.DELETE_PROJECT, project.getProjectName());
        projectRepository.delete(project);
    
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> getAllProjects() {
        User currentUser = getCurrentUser();

        // Nếu là ADMIN: Thấy tất cả
        if (currentUser.getRole() == User.Role.ADMIN) {
            return projectRepository.findAll().stream()
                    .map(this::mapToResponse).toList();
        }

        // Nếu là User thường: Chỉ thấy các dự án mình là Manager HOẶC là Member
        // Chúng ta sẽ lấy danh sách ID dự án mà user này tham gia
        List<Long> joinedProjectIds = projectMemberRepository.findByUserIdAndLeftAtIsNull(currentUser.getId())
                .stream()
                .map(pm -> pm.getProject().getId())
                .toList();

        return projectRepository.findAllById(joinedProjectIds).stream()
                .map(this::mapToResponse)
                .toList();
    }
 
    @Transactional(readOnly = true)
    public List<ProjectResponse> searchProjectByName(String name) {
        User currentUser = getCurrentUser();
        
        // Tìm kiếm dự án theo tên trước
        List<Project> searchResults = projectRepository.findByProjectNameContainingIgnoreCase(name);

        // Nếu là ADMIN: Trả về hết kết quả tìm được
        if (currentUser.getRole() == User.Role.ADMIN) {
            return searchResults.stream().map(this::mapToResponse).toList();
        }

        // Nếu không: Lọc lại, chỉ giữ những dự án mà user có tham gia
        return searchResults.stream()
                .filter(p -> projectMemberRepository.existsByProjectIdAndUserIdAndLeftAtIsNull(p.getId(), currentUser.getId()))
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProjectById(Long id) {
        User currentUser = getCurrentUser();
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PROJECT_NOT_EXISTED));

        // Kiểm tra: Nếu không phải Admin và không phải thành viên thì không được xem
        boolean isMember = projectMemberRepository.existsByProjectIdAndUserIdAndLeftAtIsNull(id, currentUser.getId());
        
        if (currentUser.getRole() != User.Role.ADMIN && !isMember) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        return mapToResponse(project);
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