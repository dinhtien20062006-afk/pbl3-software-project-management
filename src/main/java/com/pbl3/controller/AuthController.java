package com.pbl3.controller;

import com.pbl3.dto.ShowInfoRequest;
import com.pbl3.dto.SigninRequest;
import com.pbl3.dto.SignupRequest;
import com.pbl3.dto.UpdateRequest;
import com.pbl3.entity.User;
import com.pbl3.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

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
            // Lưu ý: Đảm bảo trong UserService phương thức là SignIn (viết hoa chữ S theo code cũ của bạn)
            User user = userService.SignIn(request.getUsername(), request.getPassword());
            return ResponseEntity.ok(user); 
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    // 3. Cập nhật thông tin cá nhân (Sử dụng Principal để bảo mật)
    @PutMapping("/me/update")
    public ResponseEntity<?> updateMyProfile(Principal principal, @RequestBody UpdateRequest request) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Bạn chưa đăng nhập");
        }
        try {
            String currentUsername = principal.getName();
            User updatedUser = userService.updateCurrentUserProfile(
                currentUsername, 
                request.getUsername(), 
                request.getFullName(), 
                request.getBio()
            );
            return ResponseEntity.ok(updatedUser);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 4. Lấy thông tin cá nhân hiện tại
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUserInfo(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Bạn chưa đăng nhập");
        }
        try {
            String currentUsername = principal.getName(); 
            ShowInfoRequest userInfo = userService.getUserInfoByUsername(currentUsername);
            return ResponseEntity.ok(userInfo);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}