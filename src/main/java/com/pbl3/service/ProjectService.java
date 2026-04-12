package com.pbl3.service;

import com.pbl3.dto.request.ProjectRequest;
import com.pbl3.dto.response.ProjectResponse;
import com.pbl3.entity.Project;
import com.pbl3.repository.ProjectRepository;
import org.springframework.stereotype.Service;
import com.pbl3.exception.AppException;
import com.pbl3.exception.ErrorCode;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor // Sử dụng Lombok để tự động tạo Constructor cho final fields
public class ProjectService {

    private final ProjectRepository repo;

    // ===== CREATE =====
    public ProjectResponse create(ProjectRequest dto) {
        // Validate ngày trước khi build object
        if (dto.getStartDate() != null && dto.getEndDate() != null) {
            if (dto.getEndDate().isBefore(dto.getStartDate())) {
                // Sử dụng mã lỗi INVALID_KEY hoặc thêm mã riêng cho Date nếu muốn
                throw new AppException(ErrorCode.INVALID_KEY); 
            }
        }

        // Convert DTO → Entity
        Project p = Project.builder()
                .projectName(dto.getProjectName())
                .description(dto.getDescription())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .build();

        Project saved = repo.save(p);
        return mapToResponse(saved);
    }

    // ===== GET ALL =====
    public List<ProjectResponse> getAll() {
        return repo.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ===== GET BY ID =====
    public ProjectResponse getById(Long id) {
        Project p = repo.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PROJECT_NOT_EXISTED));

        return mapToResponse(p);
    }

    // ===== UPDATE =====
    public ProjectResponse update(Long id, ProjectRequest dto) {
        Project p = repo.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PROJECT_NOT_EXISTED));

        // Validate ngày khi cập nhật
        if (dto.getStartDate() != null && dto.getEndDate() != null) {
            if (dto.getEndDate().isBefore(dto.getStartDate())) {
                throw new AppException(ErrorCode.INVALID_KEY);
            }
        }

        p.setProjectName(dto.getProjectName());
        p.setDescription(dto.getDescription());
        p.setStartDate(dto.getStartDate());
        p.setEndDate(dto.getEndDate());

        Project saved = repo.save(p);
        return mapToResponse(saved);
    }

    // ===== DELETE =====
    public void delete(Long id) {
        if (!repo.existsById(id)) {
            throw new AppException(ErrorCode.PROJECT_NOT_EXISTED);
        }
        repo.deleteById(id);
    }

    // Hàm phụ trợ dùng chung để tránh lặp code convert Entity -> Response
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
}