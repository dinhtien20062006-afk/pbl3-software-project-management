package com.pbl3.controller;

import com.pbl3.dto.request.TaskAssignmentRequest;
import com.pbl3.dto.response.TaskAssignmentResponse;
import com.pbl3.service.TaskAssignmentService;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.util.List;

@RestController
@RequestMapping("/api/assignments")
@RequiredArgsConstructor
public class TaskAssignmentController {

    private final TaskAssignmentService service;

    // Assign task
    @PostMapping
    public ResponseEntity<TaskAssignmentResponse> assign(
            @RequestBody TaskAssignmentRequest request) {

        return ResponseEntity.ok(service.assign(request));
    }

    //  Unassign
    @DeleteMapping("/{id}")
    public ResponseEntity<String> unassign(@PathVariable Long id) {

        service.unassign(id);
        return ResponseEntity.ok("Hủy giao nhiệm vụ thành công");
    }

    //  History
    @GetMapping("/task/{taskId}")
    public ResponseEntity<List<TaskAssignmentResponse>> history(
            @PathVariable Long taskId) {

        return ResponseEntity.ok(service.getHistory(taskId));
    }

    //  Reassign (optional - nên có)
    @PostMapping("/reassign")
    public ResponseEntity<String> reassign(
            @RequestParam Long assignmentId,
            @RequestParam Long newUserId) {

        service.reassign(assignmentId, newUserId);
        return ResponseEntity.ok("Giao lại nhiệm vụ thành công");
    }
}