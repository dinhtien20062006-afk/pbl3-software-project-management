package com.pbl3.service;

import com.pbl3.dto.request.SignupRequest;
import com.pbl3.entity.Role;
import com.pbl3.entity.User;
import com.pbl3.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    public class AppException extends RuntimeException {
    // Bạn có thể thêm errorCode tại đây nếu muốn
    public AppException(String message) {
        super(message);
    }
}
    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User registerUser(SignupRequest request) {

        if (userRepository.existsByUsername(request.getUsername())) {
        throw new AppException("Tên đăng nhập đã tồn tại");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException("Email đã được sử dụng");
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
                .orElseThrow(() -> new AppException("Tài khoản hoặc mật khẩu không đúng"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new AppException("Tài khoản hoặc mật khẩu không đúng");
        }
        return user;
    }
}