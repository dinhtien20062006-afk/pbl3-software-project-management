package com.pbl3.repository;

import com.pbl3.entity.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

import java.util.List;

@Repository
public interface ProjectMemberRepository 
        extends JpaRepository<ProjectMember, Long> {

    List<ProjectMember> findByProject_Id(Long projectId);

    boolean existsByProject_IdAndUser_Id(Long projectId, Long userId);

    void deleteByProject_IdAndUser_Id(Long projectId, Long userId);

    Optional<ProjectMember> findByProject_IdAndUser_Id(Long projectId, Long userId);
}