package com.pbl3.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(unique= true, nullable = false)
    private String password; 

    private String fullName;

    private String description;

    private String location;

    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    private Role role;

    public enum Role { ADMIN, PROJECT_MANAGER, MEMBER }
}