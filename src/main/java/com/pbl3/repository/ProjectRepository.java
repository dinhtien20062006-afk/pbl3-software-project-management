package com.pbl3.repository;

import com.pbl3.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

// Repository dùng để thao tác DB
// JpaRepository đã có sẵn CRUD
public interface ProjectRepository extends JpaRepository<Project, Long> {

    // Không cần viết thêm vẫn dùng được:
    // Kế thừa các hàm: save(), findAll(), findById(), deleteById()
}