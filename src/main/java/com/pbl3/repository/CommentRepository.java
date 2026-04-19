package com.pbl3.repository;

import com.pbl3.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    // Sửa từ Task_Id thành Task_TaskId
    List<Comment> findByTask_Id(Long taskId);
    List<Comment> findByUser_Id(Long userId);
}