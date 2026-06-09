package com.pbl3.repository;

import com.pbl3.entity.TeamMember;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {

    @EntityGraph(attributePaths = {"user", "projectTeam", "projectTeam.project", "projectTeam.leader"})
    List<TeamMember> findByProjectTeamId(Long teamId);

    boolean existsByProjectTeam_ProjectIdAndUserId(Long projectId, Long userId);

    boolean existsByProjectTeamIdAndUserId(Long teamId, Long userId);

    @Query("SELECT tm.projectTeam.id FROM TeamMember tm WHERE tm.user.id = :userId")
    List<Long> findAllTeamIdsByUserId(@Param("userId") Long userId);

    @EntityGraph(attributePaths = {"user", "projectTeam", "projectTeam.project", "projectTeam.leader"})
    Optional<TeamMember> findByProjectTeamIdAndUserId(Long teamId, Long userId);

    @Query("SELECT COUNT(DISTINCT tm.user.id) FROM TeamMember tm WHERE tm.projectTeam.project.manager.id = :managerId")
    long countUniqueMembersByManagerId(@Param("managerId") Long managerId);

    long countByProjectTeamId(Long teamId);

    @Query("SELECT COUNT(DISTINCT tm.user.id) FROM TeamMember tm WHERE tm.projectTeam.project.id = :projectId")
    long countUniqueMembersByProjectId(@Param("projectId") Long projectId);
}
