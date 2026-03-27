package com.pbl3.controller;

import com.pbl3.dto.ShowInfoResponse;
import com.pbl3.dto.SigninRequest;
import com.pbl3.dto.SignupRequest;
import com.pbl3.dto.UpdateRequest;
import com.pbl3.entity.User;
import com.pbl3.service.AuthService; // Import AuthService mới
import com.pbl3.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    // Khai báo cả 2 "đầu bếp"
    private final AuthService authService;
    private final UserService userService;

    // Inject cả 2 vào Constructor
    public AuthController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    // 1. Endpoint Đăng ký (Gọi sang AuthService)
    @PostMapping("/signup")
    public ResponseEntity<?> register(@RequestBody SignupRequest request) {
        try {
            User registeredUser = authService.registerUser(request);
            return new ResponseEntity<>(registeredUser, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 2. Endpoint Đăng nhập (Gọi sang AuthService)
    @PostMapping("/signin")
    public ResponseEntity<?> login(@RequestBody SigninRequest request) {
        try {
            // Lưu ý: Trong AuthService bạn nên đặt tên hàm là signIn (viết thường chữ s)
            User user = authService.SignIn(request.getUsername(), request.getPassword());
            return ResponseEntity.ok(user); 
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    // 3. Cập nhật Profile (Gọi sang UserService)
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

    // 4. Lấy thông tin cá nhân (Gọi sang UserService)
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUserInfo(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Bạn chưa đăng nhập");
        }
        try {
            String currentUsername = principal.getName(); 
            ShowInfoResponse userInfo = userService.getUserInfoByUsername(currentUsername);
            return ResponseEntity.ok(userInfo);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}