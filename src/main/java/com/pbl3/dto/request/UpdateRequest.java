package com.pbl3.dto.request;

import lombok.*;

@Getter @Setter 
@NoArgsConstructor @AllArgsConstructor @Builder
public class UpdateRequest {
    private String username;
    private String description;
    private String location;
    private String phoneNumber;
}