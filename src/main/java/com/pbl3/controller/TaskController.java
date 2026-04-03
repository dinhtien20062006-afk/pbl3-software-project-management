package com.pbl3.controller;

import com.pbl3.dto.request.TaskCreateRequest;
import com.pbl3.dto.response.ShowTaskResponse;
import com.pbl3.entity.Task;
import com.pbl3.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor // Tự động tạo constructor để inject TaskService
public class TaskController {

    private final TaskService taskService;

    // 1. Lấy danh sách Task theo Project ID
    // Endpoint: GET http://localhost:8080/api/tasks/project/{projectId}
    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<ShowTaskResponse>> getTasksByProject(@PathVariable Long projectId) {
        List<ShowTaskResponse> tasks = taskService.getTasksByProjectId(projectId);
        return ResponseEntity.ok(tasks);
    }

    // 2. Tạo mới một Task
    // Endpoint: POST http://localhost:8080/api/tasks
    @PostMapping
    public ResponseEntity<Task> createTask(@RequestBody TaskCreateRequest request) {
        Task newTask = taskService.createTask(request);
        return ResponseEntity.ok(newTask);
    }

    // 3. Cập nhật Task theo ID
    // Endpoint: PUT http://localhost:8080/api/tasks/{taskId}
    @PutMapping("/{taskId}")
    public ResponseEntity<Task> updateTask(
            @PathVariable Long taskId, 
            @RequestBody TaskCreateRequest request) {
        Task updatedTask = taskService.updateTask(taskId, request);
        return ResponseEntity.ok(updatedTask);
    }

    // 4. Xóa một Task
    // Endpoint: DELETE http://localhost:8080/api/tasks/{taskId}
    @DeleteMapping("/{taskId}")
    public ResponseEntity<String> deleteTask(@PathVariable Long taskId) {
        taskService.deleteTask(taskId);
        return ResponseEntity.ok("Xóa task thành công!");
    }
}