package com.pbl3.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Entity đại diện cho bảng users trong database
 */
@Entity
@Table(name = "users")
@Getter
@Setter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Email dùng để đăng nhập
     */
    @Column(unique = true, nullable = false)
    private String email;

    /**
     * Password đã được mã hóa (BCrypt)
     */
    @Column(nullable = false)
    private String password;

    /**
     * Role để phân quyền (ADMIN / PM / MEMBER)
     */
    @Enumerated(EnumType.STRING)
    private Role role;
}