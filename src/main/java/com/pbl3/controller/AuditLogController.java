package com.pbl3.controller;

import com.pbl3.dto.request.AuditLogRequest;
import com.pbl3.dto.response.AuditLogResponse;
import com.pbl3.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    // CREATE LOG
    @PostMapping
    public ResponseEntity<AuditLogResponse> createLog(
            @RequestBody AuditLogRequest request) {

        return ResponseEntity.ok(
                auditLogService.createLog(request)
        );
    }

    // GET LOG BY TASK
    @GetMapping("/task/{taskId}")
    public ResponseEntity<List<AuditLogResponse>> getLogsByTask(
            @PathVariable Long taskId) {

        return ResponseEntity.ok(
                auditLogService.getLogsByTask(taskId)
        );
    }

    // GET LOG BY USER
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<AuditLogResponse>> getLogsByUser(
            @PathVariable Long userId) {

        return ResponseEntity.ok(
                auditLogService.getLogsByUser(userId)
        );
    }
}