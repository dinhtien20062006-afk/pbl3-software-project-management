package com.pbl3.service;

import com.pbl3.dto.request.UpdateRequest;
import com.pbl3.dto.response.ShowInfoResponse;
import com.pbl3.entity.User;
import com.pbl3.exception.AppException;
import com.pbl3.exception.ErrorCode;
import com.pbl3.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    // 1. Tìm User theo username 
    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    // 2. Lấy thông tin hiển thị (DTO)
    public ShowInfoResponse getUserInfo(String username) {
        User user = findByUsername(username);

        return ShowInfoResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .description(user.getDescription())
                .role(user.getRole().name())
                .location(user.getLocation())
                .phoneNumber(user.getPhoneNumber())
                .build();
    }

    // 3. Cập nhật Profile (Dựa trên username hiện tại)
    public User updateCurrentUserProfile(String currentUsername, UpdateRequest request) {
        User user = findByUsername(currentUsername);

        // Nếu muốn đổi username, phải check xem username mới đã có ai dùng chưa
        if (request.getUsername() != null && !user.getUsername().equals(request.getUsername())) {
            if (userRepository.existsByUsername(request.getUsername())) {
                throw new AppException(ErrorCode.USER_EXISTED);
            }
            user.setUsername(request.getUsername());
        }

        user.setDescription(request.getDescription());
        user.setLocation(request.getLocation());
        user.setPhoneNumber(request.getPhoneNumber());

        return userRepository.save(user);
    }

    //4. Hiển thị tất cả người dùng (dành cho admin)
    public List<ShowInfoResponse> getAllUsers() {
        List<User> users = userRepository.findAll();
        return users.stream()
                .map(user -> ShowInfoResponse.builder()
                        .userId(user.getId())
                        .username(user.getUsername())
                        .fullName(user.getFullName())
                        .role(user.getRole().name())
                        .phoneNumber(user.getPhoneNumber())
                        .build())
                .collect(Collectors.toList());
    }

    //5. Cập nhật quyền người dùng (dành cho admin)
    public void updateUserRole(Long userId, User.Role newRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với ID: " + userId));

        // Không cho phép hạ quyền Admin cuối cùng để tránh lỗi hệ thống không có admin
        if (user.getRole() == User.Role.ADMIN && newRole != User.Role.ADMIN) {
            long adminCount = userRepository.findAllByRole(User.Role.ADMIN).size();
            if (adminCount <= 1) {
                throw new RuntimeException("Không thể hạ quyền Admin duy nhất của hệ thống!");
            }
        }

        user.setRole(newRole);
        userRepository.save(user);
    }

    public List<ShowInfoResponse> searchUserByUsername(String username) {
    // Tìm các user có username chứa chuỗi tìm kiếm 
    return userRepository.findByUsernameContainingIgnoreCase(username).stream()
            .map(user -> ShowInfoResponse.builder()
                    .userId(user.getId())
                    .username(user.getUsername())
                    .fullName(user.getFullName())
                    .build())
            .toList();
    }

}