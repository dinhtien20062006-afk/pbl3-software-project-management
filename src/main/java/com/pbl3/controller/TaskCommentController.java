package com.pbl3.controller;

import com.pbl3.dto.request.CommentRequest;
import com.pbl3.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TaskCommentController {

    private final TaskService taskService;

    @PostMapping
    public ResponseEntity<String> addComment(@RequestBody CommentRequest request) {
        taskService.addComment(request);
        return ResponseEntity.ok("Bình luận đã được gửi thành công!");
    }
}