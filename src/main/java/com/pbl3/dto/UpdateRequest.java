package com.pbl3.dto;
import lombok.*;

@Getter
@Setter
public class UpdateRequest {
    private String username;
    private String email;
    private String bio;
    private String fullName;
}
