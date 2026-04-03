package com.pbl3.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShowTaskResponse { // Sửa lỗi chính tả Respone -> Response
    private Long id;
    private String taskName;
    private String description;
    private String status;   // Trả về String cho Frontend dễ đọc
    private String priority; // Trả về String cho Frontend dễ đọc
    private LocalDateTime deadLine; 
}
