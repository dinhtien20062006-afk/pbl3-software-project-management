package com.pbl3.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity 
@Table(name = "projects")
@Getter 
@Setter 
@NoArgsConstructor 
@AllArgsConstructor 
@Builder 
public class Project {

    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY) 
    private Long id;

    @Column(name = "project_name") 
    private String projectName;

    private String description; 

    private LocalDateTime startDate; 

    private LocalDateTime endDate; 

    private ProjectStatus status; 

    @ManyToOne
    @JoinColumn(name = "manager_id") 
    private User manager;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProjectTeam> teams;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Task> tasks;

    @OneToMany(mappedBy = "project") 
    private List<AuditLog> auditLogs;

    public enum ProjectStatus{PLANNING, IN_PROGRESS, COMPLETED, ON_HOLD}
}