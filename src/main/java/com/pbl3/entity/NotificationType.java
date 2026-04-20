package com.pbl3.entity;

public enum NotificationType {
    TASK_CREATED,      // Khi tạo task
    TASK_UPDATED,      // Khi update thông tin task
    TASK_ASSIGNED,     // Khi được giao task
    COMMENT_ADDED,     // Khi có người bình luận
    TASK_SUBMITTED,    // Khi nộp task
    TASK_DONE,         // Khi task được duyệt xong
    DEADLINE_REMINDER  // Nhắc nhở hạn chót
}