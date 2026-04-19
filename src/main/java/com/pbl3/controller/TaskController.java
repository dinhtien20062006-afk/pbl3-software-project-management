package com.pbl3.controller;

import com.pbl3.dto.request.TaskCreateRequest;
import com.pbl3.dto.request.TaskUpdateRequest;
import com.pbl3.dto.response.ShowTaskResponse;
import com.pbl3.entity.Task;
import com.pbl3.service.ExportService;
import com.pbl3.service.TaskService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;
    private final ExportService exportService;

    // --- Các API CRUD ---
    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<ShowTaskResponse>> getTasksByProject(@PathVariable Long projectId) {
        return ResponseEntity.ok(taskService.getTasksByProjectId(projectId));
    }

    @PostMapping
    public ResponseEntity<Task> createTask(@RequestBody TaskCreateRequest request) {
        return ResponseEntity.ok(taskService.createTask(request));
    }

    @PutMapping("/{taskId}")
    public ResponseEntity<ShowTaskResponse> updateTask(@PathVariable Long taskId, @RequestBody TaskUpdateRequest request) {
        return ResponseEntity.ok(taskService.updateTask(taskId, request));
    }

    @DeleteMapping("/{taskId}")
    public ResponseEntity<String> deleteTask(@PathVariable Long taskId) {
        taskService.deleteTask(taskId);
        return ResponseEntity.ok("Xóa task thành công!");
    }

    // --- API Thống kê (Dashboard) ---
    @GetMapping("/stats/{projectId}")
    public ResponseEntity<Map<String, Long>> getStats(@PathVariable Long projectId) {
        Map<String, Long> stats = new HashMap<>();
        stats.put("completed", taskService.countByStatus(projectId, "COMPLETED"));
        stats.put("inProgress", taskService.countByStatus(projectId, "IN_PROGRESS"));
        return ResponseEntity.ok(stats);
    }

    // --- API Xuất file ---
    @GetMapping("/export/excel/{projectId}")
    public void exportToExcel(@PathVariable Long projectId, HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=tasks.xlsx");
        exportService.exportTasksToExcel(projectId, response.getOutputStream());
    }

    @GetMapping("/export/pdf/{projectId}")
    public void exportToPdf(@PathVariable Long projectId, HttpServletResponse response) throws IOException {
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=tasks.pdf");
        exportService.exportTasksToPdf(projectId, response.getOutputStream());
    }
}