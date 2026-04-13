package com.pbl3.service;

import com.pbl3.dto.response.ShowInfoResponse;
import com.pbl3.entity.User;
import com.pbl3.repository.UserRepository;
import org.springframework.stereotype.Service;

import com.pbl3.exception.AppException;
import com.pbl3.exception.ErrorCode;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }


    public User updateCurrentUserProfile(String currentUsername, String newUsername, String fullName, String bio, String Location, String avatarUrl) {
        // Tìm User dựa trên username hiện tại
        User user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // Nếu người dùng muốn đổi sang Username mới
        if (newUsername != null && !user.getUsername().equals(newUsername)) {
            // Kiểm tra xem username mới có bị trùng với ai khác không
            if (userRepository.existsByUsername(newUsername)) {
                throw new AppException(ErrorCode.USER_EXISTED);
            }
            user.setUsername(newUsername);
        }

        // Cập nhật các thông tin khác
        user.setAvatarUrl(avatarUrl);
        user.setFullName(fullName);
        user.setBio(bio);
        user.setLocation(Location);

        return userRepository.save(user);
    }

    // 2. Lấy thông tin hiển thị dựa trên Username
    public ShowInfoResponse getUserInfoByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        ShowInfoResponse info = new ShowInfoResponse();
        String baseUrl = "http://localhost:8080/uploads/";
        if (user.getAvatarUrl() != null) {
            info.setAvatarUrl(baseUrl + user.getAvatarUrl());
        } else {
            info.setAvatarUrl(baseUrl + "default-avatar.png"); // Ảnh mặc định nếu user chưa có ảnh
        }
        info.setUsername(user.getUsername());
        info.setEmail(user.getEmail());
        info.setFullName(user.getFullName());
        info.setBio(user.getBio());
        info.setLocation(user.getLocation());
        info.setStatus(user.getStatus());
        return info;
    }
}