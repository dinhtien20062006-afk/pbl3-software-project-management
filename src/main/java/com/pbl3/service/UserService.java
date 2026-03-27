package com.pbl3.service;

import com.pbl3.dto.ShowInfoResponse;
import com.pbl3.entity.User;
import com.pbl3.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User updateCurrentUserProfile(String currentUsername, String newUsername, String fullName, String bio) {
        // Tìm User dựa trên username hiện tại
        User user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new IllegalArgumentException("Người dùng không tồn tại"));

        // Nếu người dùng muốn đổi sang Username mới
        if (newUsername != null && !user.getUsername().equals(newUsername)) {
            // Kiểm tra xem username mới có bị trùng với ai khác không
            if (userRepository.existsByUsername(newUsername)) {
                throw new IllegalArgumentException("Tên đăng nhập mới đã tồn tại!");
            }
            user.setUsername(newUsername);
        }

        // Cập nhật các thông tin khác
        user.setFullName(fullName);
        user.setBio(bio);

        return userRepository.save(user);
    }

    // 2. Lấy thông tin hiển thị dựa trên Username
    public ShowInfoResponse getUserInfoByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng: " + username));

        ShowInfoResponse info = new ShowInfoResponse();
        info.setUsername(user.getUsername());
        info.setEmail(user.getEmail());
        info.setFullName(user.getFullName());
        info.setBio(user.getBio());

        return info;
    }
}