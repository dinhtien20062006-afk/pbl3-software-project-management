package com.pbl3.repository;

import com.pbl3.entity.ProjectMember;
import com.pbl3.entity.ProjectMemberId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, ProjectMemberId> {

    List<ProjectMember> findByProject_Id(Long projectId);

    boolean existsByProject_IdAndUser_Id(Long projectId, Long userId);

    void deleteByProject_IdAndUser_Id(Long projectId, Long userId);
}