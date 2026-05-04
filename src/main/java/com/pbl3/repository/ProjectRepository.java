package com.pbl3.repository;

import com.pbl3.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByProjectNameContainingIgnoreCase(String name);
    List<Project> findByManagerId(Long managerId);

    // Đếm số dự án một Manager đang quản lý (Dành cho thẻ Stat)
    long countByManagerId(Long managerId);

}