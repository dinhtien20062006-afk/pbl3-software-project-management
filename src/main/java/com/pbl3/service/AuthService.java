package com.pbl3.service;

import com.pbl3.dto.SigninRequest;
import com.pbl3.dto.SignupRequest;
import com.pbl3.entity.User;
import com.pbl3.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User registerUser(SignupRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Tên đăng nhập đã tồn tại!");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email đã được sử dụng!");
        }
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(request.getPassword()); // Thực tế nên dùng PasswordEncoder ở đây
        user.setEmail(request.getEmail());
        return userRepository.save(user);
    }

    public User signIn(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Sai tên đăng nhập!"));
        
        if (!user.getPassword().equals(password)) {
            throw new IllegalArgumentException("Sai mật khẩu!");
        }
        return user;
    }
}