package com.pbl3.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Kích hoạt một broker đơn giản để gửi tin nhắn về client
        // /topic: dành cho broadcast (nhiều người)
        // /queue: dành cho tin nhắn riêng (1 người)
        config.enableSimpleBroker("/topic", "/queue");
        
        // Tiền tố cho các yêu cầu từ client gửi lên server
        config.setApplicationDestinationPrefixes("/app");
        
        // Tiền tố cho các tin nhắn gửi đích danh người dùng (convertAndSendToUser)
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Endpoint để client Flutter/Web kết nối vào
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*") // Cho phép mọi nguồn (hoặc cấu hình cụ thể)
                .withSockJS(); // Hỗ trợ SockJS cho các trình duyệt cũ
    }
}