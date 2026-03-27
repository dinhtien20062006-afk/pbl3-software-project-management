package com.pbl3.controller;

import com.pbl3.dto.SigninRequest;
import com.pbl3.dto.SignupRequest;
import com.pbl3.dto.UpdateRequest;
import com.pbl3.entity.User;
import com.pbl3.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    // 1. Endpoint Đăng ký
    @PostMapping("/signup")
    public ResponseEntity<?> register(@RequestBody SignupRequest request) {
        try {
            User registeredUser = userService.registerUser(request);
            return new ResponseEntity<>(registeredUser, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 2. Endpoint Đăng nhập
    @PostMapping("/signin")
    public ResponseEntity<?> login(@RequestBody SigninRequest request) {
        try {
            // Gọi phương thức SignIn bạn vừa viết
            User user = userService.SignIn(request.getUsername(), request.getPassword());
            
            // Ở đây mình trả về Object User để test, thực tế sẽ trả về JWT
            return ResponseEntity.ok(user); 
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }
    @PutMapping("/{userId}")
    public ResponseEntity<?> updateProfile(@PathVariable Long userId, @RequestBody UpdateRequest request) {
        try {
            User updatedUser = userService.UpdateUserProfile(userId, request.getUsername(), request.getFullName(), request.getBio());
            return ResponseEntity.ok(updatedUser);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
