package com.pbl3.service;

import com.pbl3.dto.RegisterRequest;
import com.pbl3.entity.Role;
import com.pbl3.entity.User;
import com.pbl3.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Service xử lý logic Authentication
 */
@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Xử lý đăng ký tài khoản
     */
    public void register(RegisterRequest request) {

        // Tạo user mới
        User user = new User();
        user.setEmail(request.email);

        /**
         * Mã hóa password bằng BCrypt
         */
        user.setPassword(passwordEncoder.encode(request.password));

        // Gán role mặc định
        user.setRole(Role.MEMBER);

        // Lưu vào database
        userRepository.save(user);
    }
}