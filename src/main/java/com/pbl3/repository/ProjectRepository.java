package com.pbl3.repository;

import com.pbl3.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByProjectNameContainingIgnoreCase(String name);
    List<Project> findByManagerId(Long managerId);

    // PM: Đếm số dự án đang quản lý
    long countByManagerId(Long managerId);

    // Lấy danh sách dự án mà User làm PM hoặc là thành viên của một nhóm trong dự án đó
    @Query("SELECT DISTINCT p FROM Project p " +
        "LEFT JOIN ProjectTeam pt ON pt.project.id = p.id " +
        "LEFT JOIN TeamMember tm ON tm.projectTeam.id = pt.id " +
        "WHERE p.manager.id = :userId OR tm.user.id = :userId")
    List<Project> findAvailableProjects(@Param("userId") Long userId);

}