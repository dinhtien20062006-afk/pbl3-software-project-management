package com.pbl3.controller;

import com.pbl3.dto.request.ProjectMemberRequest;
import com.pbl3.dto.response.ProjectMemberResponse;
import com.pbl3.service.ProjectMemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/projects/{projectId}/members")
@RequiredArgsConstructor
public class ProjectMemberController {

    private final ProjectMemberService projectMemberService;

    // GET list
    @GetMapping
    public List<ProjectMemberResponse> getMembers(@PathVariable Long projectId) {
        return projectMemberService.getMembers(projectId);
    }

    // ADD
    @PostMapping
    public String addMember(
            @PathVariable Long projectId,
            @RequestBody ProjectMemberRequest request) {

        projectMemberService.addMember(projectId, request);
        return "Thêm thành viên thành công";
    }

    // DELETE
    @DeleteMapping("/{userId}")
    public String removeMember(
            @PathVariable Long projectId,
            @PathVariable Long userId) {

        projectMemberService.removeMember(projectId, userId);
        return "Xóa thành viên thành công";
    }
}