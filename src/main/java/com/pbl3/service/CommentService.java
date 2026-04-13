package com.pbl3.service;

import com.pbl3.dto.request.CommentCreateRequest;
import com.pbl3.dto.request.CommentUpdateRequest;
import com.pbl3.dto.response.CommentResponse;
import com.pbl3.entity.Comment;
import com.pbl3.entity.Task;
import com.pbl3.entity.User;
import com.pbl3.repository.CommentRepository;
import com.pbl3.repository.TaskRepository;
import com.pbl3.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    private CommentResponse mapToResponse(Comment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .taskId(comment.getTask().getId())
                .userId(comment.getUser().getId())
                .username(comment.getUser().getUsername())
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .isEdited(comment.getIsEdited())
                .build();
    }

    public CommentResponse createComment(CommentCreateRequest request, Long userId) {

        Task task = taskRepository.findById(request.getTaskId())
                .orElseThrow(() -> new RuntimeException("Task not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Comment comment = Comment.builder()
                .task(task)
                .user(user)
                .content(request.getContent())
                .createdAt(LocalDateTime.now())
                .isEdited(false)
                .build();

        return mapToResponse(commentRepository.save(comment));
    }

    public CommentResponse updateComment(Long commentId, CommentUpdateRequest request, Long userId) {

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));

        if (!comment.getUser().getId().equals(userId)) {
            throw new RuntimeException("No permission");
        }

        comment.setContent(request.getContent());
        comment.setUpdatedAt(LocalDateTime.now());
        comment.setIsEdited(true);

        return mapToResponse(commentRepository.save(comment));
    }

    public void deleteComment(Long commentId, Long userId) {

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));

        if (!comment.getUser().getId().equals(userId)) {
            throw new RuntimeException("No permission");
        }

        commentRepository.delete(comment);
    }

    public List<CommentResponse> getCommentsByTask(Long taskId) {
        return commentRepository.findByTaskId(taskId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }
}