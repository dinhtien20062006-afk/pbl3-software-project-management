package com.pbl3.controller;

import com.pbl3.dto.request.CommentCreateRequest;
import com.pbl3.dto.request.CommentUpdateRequest;
import com.pbl3.dto.response.CommentResponse;
import com.pbl3.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping
    public CommentResponse createComment(
            @RequestBody CommentCreateRequest request,
            @RequestParam Long userId
    ) {
        return commentService.createComment(request, userId);
    }

    @PutMapping("/{id}")
    public CommentResponse updateComment(
            @PathVariable Long id,
            @RequestBody CommentUpdateRequest request,
            @RequestParam Long userId
    ) {
        return commentService.updateComment(id, request, userId);
    }

    @DeleteMapping("/{id}")
    public String deleteComment(
            @PathVariable Long id,
            @RequestParam Long userId
    ) {
        commentService.deleteComment(id, userId);
        return "Deleted successfully";
    }

    @GetMapping("/task/{taskId}")
    public List<CommentResponse> getCommentsByTask(@PathVariable Long taskId) {
        return commentService.getCommentsByTask(taskId);
    }
}