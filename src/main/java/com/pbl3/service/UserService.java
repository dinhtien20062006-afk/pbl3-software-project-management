package com.pbl3.service;

import com.pbl3.dto.request.SignupRequest;
import com.pbl3.entity.Role;
import com.pbl3.entity.User;
import com.pbl3.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User registerUser(SignupRequest request) {

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Tên đăng nhập đã tồn tại");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email đã được sử dụng");
        }

        User user = new User();

        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setRole(Role.USER); // default role

        // BCrypt hash
        user.setPassword(
                passwordEncoder.encode(request.getPassword())
        );

        return userRepository.save(user);
    }

    public User SignIn(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Tài khoản hoặc mật khẩu không đúng"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("Tài khoản hoặc mật khẩu không đúng");
        }
        return user;
    }

    public User UpdateUserProfile(Long userId, String newUsername, String fullName, String bio) {
    User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("Người dùng không tồn tại"));

    if (!user.getUsername().equals(newUsername)) {
        if (userRepository.existsByUsername(newUsername)) {
            throw new IllegalArgumentException("Tên đăng nhập này đã tồn tại, vui lòng chọn tên khác!");
        }
        user.setUsername(newUsername);
    }

    user.setFullName(fullName);
    user.setBio(bio);
    return userRepository.save(user);
    }
}