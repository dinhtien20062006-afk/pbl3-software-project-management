package com.pbl3.service;

import com.pbl3.dto.ShowInfoRequest;
import com.pbl3.dto.SignupRequest;
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

    public User updateCurrentUserProfile(String currentUsername, String newUsername, String fullName, String bio) {
    // 1. Tìm User dựa trên username hiện tại đang đăng nhập
    User user = userRepository.findByUsername(currentUsername)
            .orElseThrow(() -> new IllegalArgumentException("Người dùng không tồn tại"));

    // 2. Nếu họ muốn đổi sang Username mới
    if (newUsername != null && !user.getUsername().equals(newUsername)) {
        // Kiểm tra xem username mới có bị trùng với ai khác không
        if (userRepository.existsByUsername(newUsername)) {
            throw new IllegalArgumentException("Tên đăng nhập mới đã tồn tại!");
        }
        user.setUsername(newUsername);
    }

    // 3. Cập nhật các thông tin khác
    user.setFullName(fullName);
    user.setBio(bio);

    return userRepository.save(user);
}
    
    public ShowInfoRequest getUserInfoByUsername(String username) {
    // Tìm User dựa trên username (phải dùng hàm findByUsername đã tạo ở Repository)
    User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng: " + username));
    ShowInfoRequest info = new ShowInfoRequest();
    info.setUsername(user.getUsername());
    info.setEmail(user.getEmail());
    info.setFullName(user.getFullName());
    info.setBio(user.getBio());

    return info;
    }
}