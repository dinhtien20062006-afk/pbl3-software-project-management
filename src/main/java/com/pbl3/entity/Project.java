package com.pbl3.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

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

    private LocalDate startDate; 

    private LocalDate endDate; 

    private ProjectStatus status; 

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id") 
    private User manager;

    public enum ProjectStatus{PLANNING, IN_PROGRESS, COMPLETED, ON_HOLD}
}