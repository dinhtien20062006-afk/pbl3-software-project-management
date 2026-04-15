package com.pbl3.service;

import com.pbl3.entity.Notification;
import com.pbl3.entity.NotificationType;
import com.pbl3.entity.User;
import com.pbl3.repository.NotificationRepository;
import com.pbl3.dto.response.NotificationResponse;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final JavaMailSender mailSender;

    @Transactional
    public void sendNotification(User user, String title, String content, NotificationType type) {
        // 1. Lưu vào Database
        Notification notification = Notification.builder()
                .user(user)
                .title(title)
                .content(content)
                .type(type)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();
        
        Notification savedNotification = notificationRepository.save(notification);

        // 2. Chuyển đổi sang DTO để gửi đi
        NotificationResponse response = mapToResponse(savedNotification);

        // 3. Gửi WebSocket (Gửi cả Object JSON thay vì chỉ mỗi content)
        messagingTemplate.convertAndSendToUser(
                user.getId().toString(),
                "/queue/notifications",
                response 
        );

        // 4. Gửi Email (Chỉ gửi nếu là nhắc nhở Deadline quan trọng)
        if (type == NotificationType.DEADLINE_REMINDER && user.getEmail() != null) {
            sendEmail(user.getEmail(), title, content);
        }
    }

    // Hàm lấy lịch sử thông báo cho User (Dùng cho API hiển thị danh sách)
    public List<NotificationResponse> getNotificationsForUser(Long userId) {
        // Bạn cần khai báo hàm findByUserIdOrderByCreatedAtDesc trong Repository
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // Hàm phụ trợ convert Entity sang DTO
    private NotificationResponse mapToResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .content(notification.getContent())
                .type(notification.getType().name())
                .isRead(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }

    private void sendEmail(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }
}