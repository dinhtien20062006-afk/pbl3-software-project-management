package com.pbl3.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CommentResponse {

    private Long id;

    private Long taskId;

    private Long userId;
    private String username;

    private String content;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Boolean isEdited;
}