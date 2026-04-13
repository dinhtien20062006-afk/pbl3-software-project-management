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

    private ProjectStatus status; 

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id") // Khớp với tên cột trong file SQL của bạn
    private User manager;
}