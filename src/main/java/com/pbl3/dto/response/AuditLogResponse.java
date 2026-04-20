package com.pbl3.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class AuditLogResponse {

    private Long id;

    private String action;       

    private String description;  

    private String oldValue;

    private String newValue;

    private String username;

    private LocalDateTime createdAt;
}