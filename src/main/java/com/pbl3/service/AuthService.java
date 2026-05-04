package com.pbl3.service;

import com.pbl3.dto.request.*;
import com.pbl3.entity.User;
import com.pbl3.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Collections;
import com.pbl3.exception.*;

@Service
@RequiredArgsConstructor
public class AuthService implements UserDetailsService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    // Load user phục vụ Spring Security đăng nhập qua Form
    @Override
    public UserDetails loadUserByUsername(String username)  {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
    }
    
    // Xử lý đăng ký người dùng mới
    public void registerNewUser(RegisterRequest registerRequest) {
        if (userRepository.findByUsername(registerRequest.getUsername()).isPresent()) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }

        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setFullName(registerRequest.getFullName());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        
        // Logic gán quyền: Nếu là người dùng đầu tiên thì cho làm ADMIN, ngược lại là MEMBER
        if (userRepository.count() == 0) {
            user.setRole(User.Role.ADMIN);
        } else {
            user.setRole(User.Role.MEMBER);
        }
        
        userRepository.save(user);
    }
}