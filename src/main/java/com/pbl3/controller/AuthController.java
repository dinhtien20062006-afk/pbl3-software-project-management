package com.pbl3.controller;

import com.pbl3.dto.request.SigninRequest;
import com.pbl3.dto.request.SignupRequest;
import com.pbl3.entity.User;
import com.pbl3.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    public ResponseEntity<?> register(@RequestBody SignupRequest request) {
        try {
            User registeredUser = authService.registerUser(request);
            return new ResponseEntity<>(registeredUser, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/signin")
    public ResponseEntity<?> login(@RequestBody SigninRequest request) {
        try {
            User user = authService.SignIn(request.getUsername(), request.getPassword());
            return ResponseEntity.ok(user); 
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }
}