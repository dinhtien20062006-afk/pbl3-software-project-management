package com.pbl3.dto.request;

import lombok.Data;

@Data
public class CommentCreateRequest {

    private Long taskId;
    private String content;
}