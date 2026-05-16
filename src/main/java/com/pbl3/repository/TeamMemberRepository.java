package com.pbl3.repository;

import com.pbl3.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {
    List<TeamMember> findByProjectTeamId(Long teamId);
    boolean existsByProjectTeam_ProjectIdAndUserId(Long projectId, Long userId);
    
    // Kiểm tra một user có phải là thành viên của team không
    boolean existsByProjectTeamIdAndUserId(Long teamId, Long userId);
    
    // Lấy danh sách các Team ID mà user tham gia
    @Query("SELECT tm.projectTeam.id FROM TeamMember tm WHERE tm.user.id = :userId")
    List<Long> findAllTeamIdsByUserId(@Param("userId") Long userId);

    Optional<TeamMember> findByProjectTeamIdAndUserId(Long teamId, Long userId);

    // Đếm số lượng thành viên thuộc các nhóm trong các dự án do PM quản lý
    @Query("SELECT COUNT(DISTINCT tm.user.id) FROM TeamMember tm WHERE tm.projectTeam.project.manager.id = :managerId")
    long countUniqueMembersByManagerId(@Param("managerId") Long managerId);

    // Đếm tổng số thành viên trong một nhóm cụ thể
    long countByProjectTeamId(Long teamId);

    // Đếm số lượng thành viên độc nhất (không trùng) trong toàn bộ dự án (qua các nhóm)
    @Query("SELECT COUNT(DISTINCT tm.user.id) FROM TeamMember tm WHERE tm.projectTeam.project.id = :projectId")
    long countUniqueMembersByProjectId(@Param("projectId") Long projectId);
}