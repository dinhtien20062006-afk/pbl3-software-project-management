package com.pbl3.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project; // Thêm cột này để lọc log theo dự án

    // Tên đối tượng bị tác động (Ví dụ: tên dự án "PBL3", tên task "Làm báo cáo")
    @Column(name = "target_name")
    private String targetName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user; // Người thực hiện

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type")
    private AuditActionType actionType; // Hành động

    @Column(name = "created_at")
    private LocalDateTime createdAt; // Thời gian

    public enum AuditActionType {
        CREATE_PROJECT, UPDATE_PROJECT, DELETE_PROJECT, COMPLETE_PROJECT,
        ADD_MEMBER, REMOVE_MEMBER, LEAVE_TEAM,
        CREATE_TASK, UPDATE_TASK, DELETE_TASK, SUBMIT_TASK, REVIEW_TASK, REQUEST_CHANGES, START_TASK, COMPLETE_TASK,
        CREATE_TEAM, UPDATE_TEAM, DELETE_TEAM, START_TEAM, COMPLETE_TEAM, CHANGE_TASK, EXTEND_DEADLINE
    }
}