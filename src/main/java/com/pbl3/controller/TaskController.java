package com.pbl3.controller;

import com.pbl3.dto.request.TaskCreateRequest;
import com.pbl3.dto.request.TaskUpdateRequest;
import com.pbl3.dto.response.ShowTaskResponse;
import com.pbl3.entity.Task;
import com.pbl3.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

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
}