package com.pbl3.controller;

import com.pbl3.dto.response.NotificationResponse;
import com.pbl3.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Cho phép Flutter gọi API
public class NotificationController {

    private final NotificationService notificationService;

    // Lấy danh sách thông báo của một User (Ví dụ: /api/notifications/user/1)
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<NotificationResponse>> getNotifications(@PathVariable Long userId) {
        return ResponseEntity.ok(notificationService.getNotificationsForUser(userId));
    }
}