package com.pbl3.controller;

import com.pbl3.dto.request.ProjectMemberRequest;
import com.pbl3.dto.response.ProjectMemberResponse;
import com.pbl3.service.ProjectMemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/project-members")
@RequiredArgsConstructor
public class ProjectMemberController {

    private final ProjectMemberService service;

    // Thêm thành viên vào dự án
    @PostMapping
    public ProjectMemberResponse addMember(@RequestBody ProjectMemberRequest request) {
        return service.addMember(request);
    }

    // Lấy danh sách thành viên theo projectId
    @GetMapping("/{projectId}")
    public List<ProjectMemberResponse> getMembers(@PathVariable Long projectId) {
        return service.getMembers(projectId);
    }

    // Xóa thành viên khỏi dự án
    @DeleteMapping
    public String removeMember(@RequestParam Long projectId,
                               @RequestParam Long userId) {
        service.removeMember(projectId, userId);
        return "Xóa thành viên khỏi dự án thành công";
    }
}