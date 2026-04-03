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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectMemberService {

    private final ProjectMemberRepository repo;
    private final ProjectRepository projectRepo;
    private final UserRepository userRepo;

    public ProjectMemberResponse addMember(ProjectMemberRequest req) {

        if (repo.existsByProject_IdAndUser_Id(req.getProjectId(), req.getUserId())) {
            throw new RuntimeException("Người dùng đã tồn tại trong dự án");
        }

        Project project = projectRepo.findById(req.getProjectId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy dự án"));

        User user = userRepo.findById(req.getUserId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        ProjectMember pm = ProjectMember.builder()
                .project(project)
                .user(user)
                .projectRole(req.getProjectRole())
                .joinedAt(LocalDateTime.now())
                .build();

        repo.save(pm);

        return mapToResponse(pm);
    }

    public List<ProjectMemberResponse> getMembers(Long projectId) {
        return repo.findByProject_Id(projectId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public void removeMember(Long projectId, Long userId) {
        repo.deleteByProject_IdAndUser_Id(projectId, userId);
    }

    // Mapping: Chuyển dữ liệu từ Entity -> DTO
    private ProjectMemberResponse mapToResponse(ProjectMember pm) {
        return ProjectMemberResponse.builder()
                .projectId(pm.getProject().getId())
                .userId(pm.getUser().getId())
                .projectName(pm.getProject().getProjectName())
                .username(pm.getUser().getUsername())
                .projectRole(pm.getProjectRole())
                .joinedAt(pm.getJoinedAt())
                .build();
    }
}
