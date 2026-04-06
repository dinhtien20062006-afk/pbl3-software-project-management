package com.pbl3.service;

import com.pbl3.dto.request.ProjectMemberRequest;
import com.pbl3.dto.response.ProjectMemberResponse;
import com.pbl3.entity.Project;
import com.pbl3.entity.ProjectMember;
import com.pbl3.entity.User;
import com.pbl3.repository.ProjectMemberRepository;
import com.pbl3.repository.ProjectRepository;
import com.pbl3.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectMemberService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectMemberRepository projectMemberRepository;

    // Lấy danh sách member
    public List<ProjectMemberResponse> getMembers(Long projectId) {

        projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy project"));

        return projectMemberRepository.findByProject_Id(projectId)
        .stream()
        .map(pm -> ProjectMemberResponse.builder()
                .userId(pm.getUser().getId())
                .name(pm.getUser().getFullName() + " (" + pm.getUser().getUsername() + ")")
                .role(pm.getProjectRole())
                .build()
        )
        .toList();
    }

    // Thêm member
    public void addMember(Long projectId, ProjectMemberRequest request) {

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy project"));

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        if (projectMemberRepository.existsByProject_IdAndUser_Id(projectId, request.getUserId())) {
            throw new RuntimeException("Thành viên đã tồn tại trong project");
        }

        ProjectMember pm = ProjectMember.builder()
                .project(project)
                .user(user)
                .projectRole(request.getRole()) // chú ý field mới
                .joinedAt(LocalDateTime.now())
                .build();

        projectMemberRepository.save(pm);

        System.out.println("Thêm thành viên thành công");
    }

    // Xóa member
    public void removeMember(Long projectId, Long userId) {

        if (!projectMemberRepository.existsByProject_IdAndUser_Id(projectId, userId)) {
            throw new RuntimeException("Thành viên không tồn tại trong project");
        }

        projectMemberRepository.deleteByProject_IdAndUser_Id(projectId, userId);

        System.out.println("Xóa thành viên thành công");
    }
}