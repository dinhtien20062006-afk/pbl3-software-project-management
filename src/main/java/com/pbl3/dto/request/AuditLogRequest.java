package com.pbl3.dto.request;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class AuditLogRequest {

    private String entityType; // TASK / PROJECT

    private Long entityId;

    private Long userId;

    private String actionType; // truyền string -> convert sang enum

    private String oldValue;

    private String newValue;
}
