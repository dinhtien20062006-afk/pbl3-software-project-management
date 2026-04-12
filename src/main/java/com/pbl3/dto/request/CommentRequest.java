package com.pbl3.dto.request;

import lombok.Data;

@Data
public class CommentRequest {
    private Long taskId;
    private Long userId;
    private String content;
    private String userName; // Dùng để hiển thị trong thông báo WebSocket
}