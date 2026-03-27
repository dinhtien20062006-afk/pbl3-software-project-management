package com.pbl3.controller;

import com.pbl3.dto.ShowInfoResponse;
import com.pbl3.dto.UpdateRequest;
import com.pbl3.entity.User;
import com.pbl3.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUserInfo(Principal principal) {
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Chưa đăng nhập");
        
        try {
            ShowInfoResponse userInfo = userService.getUserInfoByUsername(principal.getName());
            return ResponseEntity.ok(userInfo);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/me/update")
    public ResponseEntity<?> updateProfile(Principal principal, @RequestBody UpdateRequest request) {
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Chưa đăng nhập");
        
        try {
            User updatedUser = userService.updateCurrentUserProfile(
                principal.getName(), request.getUsername(), request.getFullName(), request.getBio()
            );
            return ResponseEntity.ok(updatedUser);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}