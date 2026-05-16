package com.pbl3.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

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
    private LocalDate deadline; // Deadline riêng cho nhóm

    @ManyToOne
    @JoinColumn(name = "project_id")
    private Project project;

    @ManyToOne
    @JoinColumn(name = "leader_id")
    private User leader; // Trưởng nhóm

    private TeamStatus status;

    public enum TeamStatus { PLANNING, IN_PROGRESS, COMPLETED}
}