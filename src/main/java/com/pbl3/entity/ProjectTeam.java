package com.pbl3.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
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
    private LocalDate deadline; // Deadline riêng cho nhóm

    @ManyToOne
    @JoinColumn(name = "project_id")
    private Project project;

    @ManyToOne
    @JoinColumn(name = "leader_id")
    private User leader; // Trưởng nhóm

    @OneToMany(mappedBy = "projectTeam")
    private List<ProjectMember> members; // Các thành viên thuộc nhóm này

    private TeamStatus status;

    public enum TeamStatus { PLANNING, IN_PROGRESS, COMPLETED}
}