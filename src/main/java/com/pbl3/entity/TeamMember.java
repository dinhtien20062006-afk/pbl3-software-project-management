package com.pbl3.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "team_members")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TeamMember {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) 
    private Long id;

    @ManyToOne
    @JoinColumn(name = "team_id")
    private ProjectTeam projectTeam;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private String memberRole; // e.g., DEVELOPER, TESTER
    private LocalDate joinedAt;
}