package com.pbl3.service;

import com.pbl3.dto.request.CreateProjectRequest;
import com.pbl3.dto.request.UpdateProjectRequest;
import com.pbl3.dto.response.ProjectResponse;
import com.pbl3.entity.Project;
import com.pbl3.entity.ProjectStatus;
import com.pbl3.entity.User;
import com.pbl3.entity.Role;
import com.pbl3.repository.ProjectRepository;
import com.pbl3.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    //  Lấy user hiện tại
    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));
    }

    // ================= CREATE =================
    public ProjectResponse createProject(CreateProjectRequest request) {

        User currentUser = getCurrentUser();

        //  Chỉ PM mới được tạo
        if (currentUser.getRole() != Role.PROJECT_MANAGER) {
            throw new RuntimeException("Chỉ Project Manager mới được tạo project");
        }

        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new RuntimeException("Ngày kết thúc phải sau ngày bắt đầu");
        }

        Project project = new Project();
        project.setProjectName(request.getProjectName());
        project.setDescription(request.getDescription());
        project.setStartDate(request.getStartDate());
        project.setEndDate(request.getEndDate());
        project.setStatus(ProjectStatus.PLANNING);

        //  SET MANAGER
        project.setManager(currentUser);

        projectRepository.save(project);

        System.out.println("Tạo project thành công");

        return mapToResponse(project);
    }

    // ================= UPDATE =================
    public ProjectResponse updateProject(Long id, UpdateProjectRequest request) {

        User currentUser = getCurrentUser();

        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy project"));

        //  Chỉ manager mới được update
        if (!project.getManager().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Bạn không có quyền update project này");
        }

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
                throw new RuntimeException("Ngày kết thúc không hợp lệ");
            }
        }

        projectRepository.save(project);

        System.out.println("Cập nhật project thành công");

        return mapToResponse(project);
    }

    // ================= DELETE =================
    public void deleteProject(Long id) {

        User currentUser = getCurrentUser();

        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy project"));

        //  Chỉ manager mới được xóa
        if (!project.getManager().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Bạn không có quyền xóa project này");
        }

        projectRepository.delete(project);

        System.out.println("Xóa project thành công");
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
                .orElseThrow(() -> new RuntimeException("Không tìm thấy project"));

        return mapToResponse(project);
    }

    // ================= SEARCH =================
    public List<Project> searchProjectByName(String name) {
        return projectRepository.findByProjectNameContainingIgnoreCase(name);
    }

    // ================= MAP DTO =================
    private ProjectResponse mapToResponse(Project project) {

        return new ProjectResponse(
                project.getId(),
                project.getProjectName(),
                project.getDescription(),
                project.getStartDate(),
                project.getEndDate(),
                project.getStatus()
        );
    }
}