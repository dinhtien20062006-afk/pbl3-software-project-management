package com.pbl3.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "project_members")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectMember {

    @Id
    @ManyToOne
    @GeneratedValue(strategy = GenerationType.IDENTITY) 
    private Long id;
    
    @JoinColumn(name = "project_id")
    private Project project;

    @Id
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "project_role")
    private String projectRole;

    @Column(name = "joined_at")
    private LocalDateTime joinedAt;
}