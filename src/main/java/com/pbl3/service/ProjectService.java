package com.pbl3.service;

import com.pbl3.dto.request.CreateProjectRequest;
import com.pbl3.dto.request.UpdateProjectRequest;
import com.pbl3.dto.request.AuditLogRequest;
import com.pbl3.dto.response.ProjectResponse;
import com.pbl3.entity.*;
import com.pbl3.repository.ProjectRepository;
import com.pbl3.repository.UserRepository;
import com.pbl3.exception.AppException;
import com.pbl3.exception.ErrorCode;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    //  Lấy user hiện tại
    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        return userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    // ================= CREATE =================
    public ProjectResponse createProject(CreateProjectRequest request) {

        User currentUser = getCurrentUser();

        if (currentUser.getRole() != Role.PROJECT_MANAGER) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }

        Project project = new Project();
        project.setProjectName(request.getProjectName());
        project.setDescription(request.getDescription());
        project.setStartDate(request.getStartDate());
        project.setEndDate(request.getEndDate());
        project.setStatus(ProjectStatus.PLANNING);
        project.setManager(currentUser);

        projectRepository.save(project);

        //  LOG
        auditLogService.createLog(
                AuditLogRequest.builder()
                        .entityType("PROJECT")
                        .entityId(project.getId())
                        .userId(currentUser.getId())
                        .actionType("CREATE_PROJECT")
                        .oldValue("N/A")
                        .newValue(project.getProjectName())
                        .build()
        );

        return mapToResponse(project);
    }

    // ================= UPDATE =================
    public ProjectResponse updateProject(Long id, UpdateProjectRequest request) {

        User currentUser = getCurrentUser();

        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PROJECT_NOT_EXISTED));

        if (!project.getManager().getId().equals(currentUser.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        //  lưu old value
        String oldValue = project.getProjectName();

        if (request.getProjectName() != null) {
            project.setProjectName(request.getProjectName());
        }

        if (request.getDescription() != null) {
            project.setDescription(request.getDescription());
        }

        if (request.getStartDate() != null) {
            project.setStartDate(request.getStartDate());
        }

        if (request.getEndDate() != null) {
            project.setEndDate(request.getEndDate());
        }

        if (request.getStatus() != null) {
            project.setStatus(request.getStatus());
        }

        if (project.getStartDate() != null && project.getEndDate() != null) {
            if (project.getEndDate().isBefore(project.getStartDate())) {
                throw new AppException(ErrorCode.INVALID_KEY);
            }
        }

        projectRepository.save(project);

        //  LOG
        auditLogService.createLog(
                AuditLogRequest.builder()
                        .entityType("PROJECT")
                        .entityId(project.getId())
                        .userId(currentUser.getId())
                        .actionType("UPDATE_PROJECT")
                        .oldValue(oldValue)
                        .newValue(project.getProjectName())
                        .build()
        );

        return mapToResponse(project);
    }

    // ================= DELETE =================
    public void deleteProject(Long id) {

        User currentUser = getCurrentUser();

        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PROJECT_NOT_EXISTED));

        if (!project.getManager().getId().equals(currentUser.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        String oldValue = project.getProjectName();

        projectRepository.delete(project);

        //  LOG
        auditLogService.createLog(
                AuditLogRequest.builder()
                        .entityType("PROJECT")
                        .entityId(id)
                        .userId(currentUser.getId())
                        .actionType("DELETE_PROJECT")
                        .oldValue(oldValue)
                        .newValue("DELETED")
                        .build()
        );
    }

    // ================= GET ALL =================
    public List<ProjectResponse> getAllProjects() {
        return projectRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // ================= GET BY ID =================
    public ProjectResponse getProjectById(Long id) {

        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PROJECT_NOT_EXISTED));

        return mapToResponse(project);
    }

    public List<Project> searchProjectByName(String name) {
        return projectRepository.findByProjectNameContainingIgnoreCase(name);
    }

    // ================= MAP =================
    private ProjectResponse mapToResponse(Project project) {
        return ProjectResponse.builder()
                .id(project.getId())
                .projectName(project.getProjectName())
                .description(project.getDescription())
                .startDate(project.getStartDate())
                .endDate(project.getEndDate())
                .status(project.getStatus())
                .build();
    }

    // ================= STATISTICS =================
    public ProjectStatistics getProjectStatistics(Long projectId) {

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new AppException(ErrorCode.PROJECT_NOT_EXISTED));

        List<Task> tasks = project.getTasks();

        if (tasks.isEmpty()) {
            return ProjectStatistics.builder()
                    .totalTasks(0L)
                    .completedTasks(0L)
                    .completionPercentage(0.0)
                    .build();
        }

        long total = tasks.size();
        long completed = tasks.stream()
                .filter(t -> t.getStatus() == TaskStatus.DONE)
                .count();

        double percent = ((double) completed / total) * 100;

        return ProjectStatistics.builder()
                .totalTasks(total)
                .completedTasks(completed)
                .completionPercentage(percent)
                .build();
    }
}