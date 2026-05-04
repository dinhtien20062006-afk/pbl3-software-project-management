package com.pbl3.repository;

import com.pbl3.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

    // Lấy tất cả thành viên ĐANG ở trong dự án (leftAt is null)
    List<ProjectMember> findByProjectIdAndLeftAtIsNull(Long projectId);

    // Kiểm tra xem user có đang là thành viên hoạt động của dự án không
    boolean existsByProjectIdAndUserIdAndLeftAtIsNull(Long projectId, Long userId);

    // Tìm bản ghi thành viên đang hoạt động
    Optional<ProjectMember> findByProjectIdAndUserIdAndLeftAtIsNull(Long projectId, Long userId);

    // Lấy tất cả các bản ghi member mà một User đang tham gia
    List<ProjectMember> findByUserIdAndLeftAtIsNull(Long userId);
    
    // Đếm số lượng thành viên đang hoạt động trong project
    long countByProjectIdAndLeftAtIsNull(Long projectId);

    // Xóa tất cả thành viên của một dự án (dùng khi xóa dự án)
    void deleteByProjectId(Long projectId);
}