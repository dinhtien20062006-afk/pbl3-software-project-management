package com.pbl3.repository;

import com.pbl3.entity.ProjectTeam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProjectTeamRepository extends JpaRepository<ProjectTeam, Long> {
    // Tìm các nhóm thuộc một dự án
    List<ProjectTeam> findByProjectId(Long projectId);
    
    // Tìm các nhóm mà một User đang làm Leader
    List<ProjectTeam> findByLeaderId(Long leaderId);
    long countByProjectId(Long projectId);

    @Query("SELECT COUNT(pt) FROM ProjectTeam pt WHERE pt.project.manager.id = :managerId")
    long countByProjectManagerId(@Param("managerId") Long managerId);

}