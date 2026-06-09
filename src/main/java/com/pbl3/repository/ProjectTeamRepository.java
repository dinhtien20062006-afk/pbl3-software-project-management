package com.pbl3.repository;

import com.pbl3.entity.Project;
import com.pbl3.entity.ProjectTeam;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectTeamRepository extends JpaRepository<ProjectTeam, Long> {

    @Override
    @EntityGraph(attributePaths = {"project", "project.manager", "leader"})
    List<ProjectTeam> findAll();

    @Override
    @EntityGraph(attributePaths = {"project", "project.manager", "leader"})
    List<ProjectTeam> findAllById(Iterable<Long> ids);

    @Override
    @EntityGraph(attributePaths = {"project", "project.manager", "leader"})
    Optional<ProjectTeam> findById(Long id);

    // Tim cac nhom thuoc mot du an. Nap san project, manager, leader de tranh N+1 query khi hien thi Grid.
    @EntityGraph(attributePaths = {"project", "project.manager", "leader"})
    List<ProjectTeam> findByProjectId(Long projectId);

    @EntityGraph(attributePaths = {"project", "project.manager", "leader"})
    List<ProjectTeam> findByLeaderId(Long leaderId);

    @EntityGraph(attributePaths = {"project", "project.manager", "leader"})
    @Query("SELECT DISTINCT pt FROM ProjectTeam pt " +
           "LEFT JOIN TeamMember tm ON tm.projectTeam.id = pt.id " +
           "WHERE pt.project.manager.id = :userId " +
           "OR pt.leader.id = :userId " +
           "OR tm.user.id = :userId")
    List<ProjectTeam> findTeamsRelatedToUser(@Param("userId") Long userId);

    @EntityGraph(attributePaths = {"project", "project.manager", "leader"})
    @Query("SELECT pt FROM ProjectTeam pt WHERE pt.project.manager.id = :managerId")
    List<ProjectTeam> findByProjectManagerId(@Param("managerId") Long managerId);


    @EntityGraph(attributePaths = {"project", "project.manager", "leader"})
    List<ProjectTeam> findByStatus(ProjectTeam.TeamStatus status);

    @EntityGraph(attributePaths = {"project", "project.manager", "leader"})
    @Query("SELECT pt FROM ProjectTeam pt " +
           "WHERE pt.status = :teamStatus " +
           "AND pt.project.status = :projectStatus " +
           "AND pt.project.startDate <= :now")
    List<ProjectTeam> findTeamsNeedAutoStart(@Param("teamStatus") ProjectTeam.TeamStatus teamStatus,
                                             @Param("projectStatus") Project.ProjectStatus projectStatus,
                                             @Param("now") java.time.LocalDateTime now);

    long countByProjectId(Long projectId);

    @Query("SELECT COUNT(pt) FROM ProjectTeam pt WHERE pt.project.manager.id = :managerId")
    long countByProjectManagerId(@Param("managerId") Long managerId);
}
