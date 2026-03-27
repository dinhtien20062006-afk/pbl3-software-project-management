package com.pbl3.controller;

import com.pbl3.dto.request.ProjectRequest;
import com.pbl3.dto.response.ProjectResponse;
import com.pbl3.service.ProjectService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController // API trả JSON
@RequestMapping("/api/projects") // Base URL
public class ProjectController {

    private final ProjectService service;

    public ProjectController(ProjectService service) {
        this.service = service;
    }

    // ===== CREATE =====
    @PostMapping
    public ProjectResponse create(@RequestBody ProjectRequest dto) {

        // Nhận JSON → chuyển thành DTO
        return service.create(dto);
    }

    // ===== GET ALL =====
    @GetMapping
    public List<ProjectResponse> getAll() {
        return service.getAll();
    }

    // ===== GET BY ID =====
    @GetMapping("/{id}")
    public ProjectResponse getById(@PathVariable Long id) {

        // Lấy id từ URL
        return service.getById(id);
    }

    // ===== UPDATE =====
    @PutMapping("/{id}")
    public ProjectResponse update(@PathVariable Long id,
                                 @RequestBody ProjectRequest dto) {

        return service.update(id, dto);
    }

    // ===== DELETE =====
    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id) {

        service.delete(id);
        return "Deleted!";
    }
}