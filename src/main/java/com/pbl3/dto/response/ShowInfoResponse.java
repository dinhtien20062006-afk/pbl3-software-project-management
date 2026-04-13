package com.pbl3.dto.response;
import com.pbl3.entity.UserStatus;

import lombok.*;
@Getter
@Setter
public class ShowInfoResponse {
    public String username;
    public String email;
    public String bio;
    public String fullName;
    public String Location;
    public String avatarUrl;
    public UserStatus status;
}