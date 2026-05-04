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

    // Thống kê số lượng dự án theo trạng thái (Dành cho Pie Chart của Admin)
    @Query("SELECT p.status, COUNT(p) FROM Project p GROUP BY p.status")
    List<Object[]> countProjectsGroupByStatus();

    // Lấy các dự án mà User là Manager (Dành cho Grid "Active Projects" của Manager)
    @Query("SELECT p FROM Project p WHERE p.manager.id = :managerId AND p.status != 'COMPLETED'")
    List<Project> findActiveProjectsByManager(@Param("managerId") Long managerId);

    // Đếm số dự án một Manager đang quản lý (Dành cho thẻ Stat)
    long countByManagerId(Long managerId);

}