package com.pbl3.dto.request;

import lombok.*;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamCreateRequest {
    private Long projectId;
    private String teamName;
    private String description;
    private LocalDate deadline;
    private Long leaderId; // User ID của người làm Leader nhóm
    private List<Long> memberIds; // Danh sách ID của các ProjectMember được chọn vào nhóm
}