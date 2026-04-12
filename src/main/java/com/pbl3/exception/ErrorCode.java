package com.pbl3.exception;

public enum ErrorCode {
    // --- AUTHENTICATION & USER (10xx) ---
    USER_EXISTED(1001, "Tên đăng nhập đã tồn tại"),
    INVALID_KEY(1002, "Lỗi không xác định"),
    USER_NOT_EXISTED(1003, "Người dùng không tồn tại"),
    UNAUTHENTICATED(1004, "Chưa đăng nhập"),
    EMAIL_EXISTED(1005, "Email đã tồn tại"),
    UNAUTHORIZED(1006, "Bạn không có quyền thực hiện hành động này"),

    // --- PROJECT (20xx) ---
    PROJECT_NOT_EXISTED(2001, "Dự án không tồn tại"),
    PROJECT_ARCHIVED(2002, "Dự án đã được lưu trữ, không thể chỉnh sửa"),

    // --- TASK (30xx) ---
    TASK_NOT_EXISTED(3001, "Công việc không tồn tại"),
    INVALID_TASK_STATUS(3002, "Trạng thái công việc không hợp lệ"),
    INVALID_TASK_PRIORITY(3003, "Mức độ ưu tiên không hợp lệ"),
    TASK_DEADLINE_INVALID(3004, "Hạn chót không được trước ngày bắt đầu"),

    // --- COMMENT & SYSTEM (40xx) ---
    COMMENT_NOT_EXISTED(4001, "Bình luận không tồn tại"),
    UNCATEGORIZED_EXCEPTION(9999, "Lỗi hệ thống chưa xác định");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() { return code; }
    public String getMessage() { return message; }
}