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
@Builder // Tạo object kiểu builder
public class Project {

    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY) 
    private Long id;

    @Column(name = "project_name") 
    // Map với cột project_name trong DB
    private String projectName;

    private String description; 

    private LocalDate startDate; 

    private LocalDate endDate; 

    @Enumerated(EnumType.STRING) 
    @Builder.Default
    private ProjectStatus status = ProjectStatus.PLANNING; 
    // Trạng thái mặc định
}