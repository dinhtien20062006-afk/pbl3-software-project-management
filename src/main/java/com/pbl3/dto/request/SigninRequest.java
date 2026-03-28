package com.pbl3.dto.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SigninRequest {
    private String username;
    private String password;
    private String email;
}