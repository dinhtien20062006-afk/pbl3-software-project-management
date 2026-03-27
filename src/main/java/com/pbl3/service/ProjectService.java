package com.pbl3.service;

import com.pbl3.dto.request.ProjectRequest;
import com.pbl3.dto.response.ProjectResponse;
import com.pbl3.entity.Project;
import com.pbl3.repository.ProjectRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service // Xử lý logic
public class ProjectService {

    private final ProjectRepository repo;

    // Constructor injection
    public ProjectService(ProjectRepository repo) {
        this.repo = repo;
    }

    // ===== CREATE =====
    public ProjectResponse create(ProjectRequest dto) {

        // Convert DTO → Entity
        Project p = Project.builder()
                .projectName(dto.getProjectName())
                .description(dto.getDescription())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .build();

        // Validate ngày
        if (p.getStartDate() != null && p.getEndDate() != null) {
            if (p.getEndDate().isBefore(p.getStartDate())) {
                throw new RuntimeException("Ngày không hợp lệ");
            }
        }

        Project saved = repo.save(p);

        // Convert Entity → Response
        return ProjectResponse.builder()
                .id(saved.getId())
                .projectName(saved.getProjectName())
                .description(saved.getDescription())
                .startDate(saved.getStartDate())
                .endDate(saved.getEndDate())
                .status(saved.getStatus())
                .build();
    }

    // ===== GET ALL =====
    public List<ProjectResponse> getAll() {

        return repo.findAll()
                .stream() // duyệt list
                .map(p -> ProjectResponse.builder()
                        .id(p.getId())
                        .projectName(p.getProjectName())
                        .description(p.getDescription())
                        .startDate(p.getStartDate())
                        .endDate(p.getEndDate())
                        .status(p.getStatus())
                        .build())
                .collect(Collectors.toList());
    }

    // ===== GET BY ID =====
    public ProjectResponse getById(Long id) {

        Project p = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy"));

        return ProjectResponse.builder()
                .id(p.getId())
                .projectName(p.getProjectName())
                .description(p.getDescription())
                .startDate(p.getStartDate())
                .endDate(p.getEndDate())
                .status(p.getStatus())
                .build();
    }

    // ===== UPDATE =====
    public ProjectResponse update(Long id, ProjectRequest dto) {

        Project p = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy"));

        // Update dữ liệu
        p.setProjectName(dto.getProjectName());
        p.setDescription(dto.getDescription());
        p.setStartDate(dto.getStartDate());
        p.setEndDate(dto.getEndDate());

        Project saved = repo.save(p);

        return ProjectResponse.builder()
                .id(saved.getId())
                .projectName(saved.getProjectName())
                .description(saved.getDescription())
                .startDate(saved.getStartDate())
                .endDate(saved.getEndDate())
                .status(saved.getStatus())
                .build();
    }

    // ===== DELETE =====
    public void delete(Long id) {
        repo.deleteById(id);
    }
}