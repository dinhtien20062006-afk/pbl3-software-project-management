package com.pbl3.service;

import com.pbl3.dto.request.ProjectMemberRequest;
import com.pbl3.dto.response.ProjectMemberResponse;
import com.pbl3.entity.NotificationType;
import com.pbl3.entity.Notification; 
import com.pbl3.repository.NotificationRepository;
import com.pbl3.entity.Project;
import com.pbl3.entity.ProjectMember;
import com.pbl3.entity.User;
import com.pbl3.repository.ProjectMemberRepository;
import com.pbl3.repository.ProjectRepository;
import com.pbl3.repository.UserRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectMemberService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final NotificationRepository notificationRepository;

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

        ProjectMember pm = projectMemberRepository
                .findByProject_IdAndUser_Id(projectId, userId)
                .orElseThrow(() -> new RuntimeException("Thành viên không tồn tại"));

        pm.setLeftAt(LocalDateTime.now());
        projectMemberRepository.save(pm);

        System.out.println("Member bị xóa khỏi project");
    }

    // member rời project
    public void leaveProject(Long projectId) {

        String username = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

        ProjectMember pm = projectMemberRepository
                .findByProject_IdAndUser_Id(projectId, user.getId())
                .orElseThrow(() -> new RuntimeException("Bạn không thuộc project này"));

        if (pm.getLeftAt() != null) {
            throw new RuntimeException("Bạn đã rời project rồi");
        }

        pm.setLeftAt(LocalDateTime.now());

        projectMemberRepository.save(pm);

        sendLeaveNotification(pm);

        System.out.println("User tự rời project");
    }

    private void sendLeaveNotification(ProjectMember pm) {

        Project project = pm.getProject();
        User user = pm.getUser();

        List<ProjectMember> members = projectMemberRepository
                .findByProject_Id(project.getId());

        for (ProjectMember m : members) {

            if (m.getLeftAt() == null) { 
                Notification noti = new Notification();
                noti.setUser(m.getUser());
                noti.setTitle("Thành viên rời project");
                noti.setContent(user.getUsername() + " đã rời khỏi project " + project.getProjectName());
                noti.setType(NotificationType.SYSTEM); 
                notificationRepository.save(noti);
            }
        }
    }
}
