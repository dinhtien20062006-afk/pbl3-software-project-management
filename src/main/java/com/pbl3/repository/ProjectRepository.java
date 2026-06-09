package com.pbl3.repository;

import com.pbl3.entity.Project;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    @Override
    @EntityGraph(attributePaths = {"manager"})
    List<Project> findAll();

    @Override
    @EntityGraph(attributePaths = {"manager"})
    Optional<Project> findById(Long id);

    @EntityGraph(attributePaths = {"manager"})
    List<Project> findByProjectNameContainingIgnoreCase(String name);

    @EntityGraph(attributePaths = {"manager"})
    List<Project> findByManagerId(Long managerId);


    @EntityGraph(attributePaths = {"manager"})
    List<Project> findByStatus(Project.ProjectStatus status);

    @EntityGraph(attributePaths = {"manager"})
    List<Project> findByStatusAndStartDateLessThanEqual(Project.ProjectStatus status, java.time.LocalDateTime now);

    // PM: Dem so du an dang quan ly
    long countByManagerId(Long managerId);

    // Lay danh sach du an ma User lam PM hoac la thanh vien cua mot nhom trong du an do.
    // EntityGraph nap san manager de tranh N+1 khi map sang ProjectResponse.
    @EntityGraph(attributePaths = {"manager"})
    @Query("SELECT DISTINCT p FROM Project p " +
           "LEFT JOIN ProjectTeam pt ON pt.project.id = p.id " +
           "LEFT JOIN TeamMember tm ON tm.projectTeam.id = pt.id " +
           "WHERE p.manager.id = :userId OR tm.user.id = :userId")
    List<Project> findAvailableProjects(@Param("userId") Long userId);

    @EntityGraph(attributePaths = {"manager"})
    @Query("SELECT DISTINCT p FROM Project p " +
           "LEFT JOIN ProjectTeam pt ON pt.project.id = p.id " +
           "LEFT JOIN TeamMember tm ON tm.projectTeam.id = pt.id " +
           "WHERE LOWER(p.projectName) LIKE LOWER(CONCAT('%', :name, '%')) " +
           "AND (p.manager.id = :userId OR tm.user.id = :userId)")
    List<Project> findAvailableProjectsByName(@Param("userId") Long userId, @Param("name") String name);
}
