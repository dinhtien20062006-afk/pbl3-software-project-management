package com.pbl3.config;

import com.vaadin.flow.spring.security.VaadinWebSecurity;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import com.pbl3.view.LoginView; 

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig extends VaadinWebSecurity {

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // Cho phép truy cập vào resource của Vaadin và trang đăng ký
        http.authorizeHttpRequests(auth -> 
            auth.requestMatchers("/register").permitAll()
        );

        super.configure(http);
        
        // Thiết lập trang login mặc định bằng Vaadin View
        setLoginView(http, LoginView.class);

        http.formLogin(form -> form.defaultSuccessUrl("/", true));
    }
}