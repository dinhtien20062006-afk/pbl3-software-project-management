package com.pbl3.service;

import com.pbl3.dto.request.SignupRequest;
import com.pbl3.entity.Role;
import com.pbl3.entity.User;
import com.pbl3.repository.UserRepository;

import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.pbl3.exception.AppException;
import com.pbl3.exception.ErrorCode;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User registerUser(SignupRequest request) {

        if (userRepository.existsByUsername(request.getUsername())) {
        throw new AppException(ErrorCode.USER_EXISTED);
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }

        User user = new User();

        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setRole(Role.MEMBER); // default role

        // BCrypt hash
        user.setPassword(
                passwordEncoder.encode(request.getPassword())
        );

        return userRepository.save(user);
    }

    public User SignIn(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElse(null);

        if (user != null && !user.getStatus().canLogin()) {
        throw new DisabledException("Tài khoản đang bị khóa hoặc không khả dụng. Trạng thái hiện tại: " + user.getStatus());
        }
        
        User user1 = userRepository.findByEmail(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        User authenticatedUser = user != null ? user : user1;

        if (!passwordEncoder.matches(password, authenticatedUser.getPassword())) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        return authenticatedUser;
    }
}