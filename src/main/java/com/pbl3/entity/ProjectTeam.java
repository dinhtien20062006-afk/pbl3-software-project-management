package com.pbl3.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import java.util.List;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectTeam {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String teamName;
    private String description;
    private LocalDateTime deadline; // Deadline riêng cho nhóm

    private LocalDateTime requestedDeadline; // Deadline mới Leader đề xuất với Project Manager

    @Column(length = 1000)
    private String deadlineRequestReason; // Lý do Leader xin đổi deadline

    @ManyToOne
    @JoinColumn(name = "project_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Project project;

    @ManyToOne
    @JoinColumn(name = "leader_id")
    private User leader; // Trưởng nhóm

    private TeamStatus status;

    @OneToMany(mappedBy = "projectTeam", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TeamMember> members;

    @OneToMany(mappedBy = "projectTeam", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Task> tasks;

    public enum TeamStatus { PLANNING, IN_PROGRESS, COMPLETED}
}