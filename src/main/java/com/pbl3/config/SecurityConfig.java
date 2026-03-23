package com.pbl3.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // Định nghĩa cơ chế mã hóa mật khẩu
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Tắt CSRF để thuận tiện cho việc gọi API từ công cụ test (Postman)
            .csrf(csrf -> csrf.disable())
            
            // Cấu hình phân quyền
            .authorizeHttpRequests(auth -> auth
                // Các đường dẫn không cần đăng nhập (Trang chủ, Đăng ký, Đăng nhập, CSS/JS)
                .requestMatchers("/", "/register", "/login", "/css/**", "/js/**").permitAll()
                
                // Chỉ Project Manager mới được tạo hoặc xóa dự án
                .requestMatchers("/projects/create/**", "/projects/delete/**").hasRole("PROJECT_MANAGER")
                
                // Các tính năng liên quan đến Admin hệ thống
                .requestMatchers("/admin/**").hasRole("ADMIN")
                
                // Tất cả các request khác (như Dashboard, Task) yêu cầu phải đăng nhập
                .anyRequest().authenticated()
            )
            
            // Cấu hình giao diện đăng nhập
            .formLogin(form -> form
                .loginPage("/login")               // Đường dẫn trang login do Tiến làm
                .defaultSuccessUrl("/dashboard")    // Đăng nhập xong thì vào đây
                .permitAll()
            )
            
            // Cấu hình đăng xuất
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            );

        return http.build();
    }
}