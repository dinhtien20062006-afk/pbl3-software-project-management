package com.pbl3.dto.response;

import lombok.*;

@Getter @Setter 
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ShowInfoResponse {
    private Long userId;
    private String username;
    private String fullName;
    private String role; 
    private String description;
    private String location;
    private String phoneNumber;
}