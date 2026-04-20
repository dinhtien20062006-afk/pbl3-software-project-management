package com.pbl3.controller;

import com.pbl3.dto.request.CreateProjectRequest;
import com.pbl3.dto.request.UpdateProjectRequest;
import com.pbl3.dto.response.ProjectResponse;
import com.pbl3.entity.Project;
import com.pbl3.service.ProjectService;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    // CREATE
    @PostMapping
    public ProjectResponse create( @RequestBody CreateProjectRequest request) {

        return projectService.createProject(request);
    }

    // UPDATE
    @PutMapping("/{id}")
    public ProjectResponse update(
            @PathVariable Long id,
            @RequestBody UpdateProjectRequest request) {

        return projectService.updateProject(id, request);
    }

    // GET ALL
    @GetMapping
    public List<ProjectResponse> getAll() {
        return projectService.getAllProjects();
    }

    // GET BY ID
    @GetMapping("/{id}")
    public ProjectResponse getById(@PathVariable Long id) {
        return projectService.getProjectById(id);
    }

    // DELETE
    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id) {
        projectService.deleteProject(id);
        return "Xóa project thành công";
    }

    // SEARCH
    @GetMapping("/search")
    public ResponseEntity<List<Project>> searchProject(@RequestParam String name) {
        return ResponseEntity.ok(projectService.searchProjectByName(name));
    }
}