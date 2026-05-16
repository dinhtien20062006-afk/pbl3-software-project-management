package com.pbl3.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum ErrorCode {
    USER_EXISTED(1001, "Tên đăng nhập đã tồn tại", HttpStatus.BAD_REQUEST),
    USER_NOT_EXISTED(1003, "Người dùng không tồn tại", HttpStatus.NOT_FOUND),
    UNAUTHENTICATED(1004, "Chưa đăng nhập", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(1006, "Bạn không có quyền thực hiện hành động này", HttpStatus.FORBIDDEN),
    UNCATEGORIZED_EXCEPTION(9999, "Lỗi hệ thống chưa xác định", HttpStatus.INTERNAL_SERVER_ERROR),
    PROJECT_NOT_EXISTED(2000, "Dự án không tồn tại", HttpStatus.NOT_FOUND),
    MEMBER_ALREADY_EXISTED(2002, "Thành viên đã tồn tại trong dự án", HttpStatus.BAD_REQUEST),
    MEMBER_NOT_EXISTED(2003, "Thành viên không tồn tại trong dự án", HttpStatus.NOT_FOUND),
    CANNOT_DELETE_ACTIVE_PROJECT(2004, "Không thể xóa dự án đang hoạt động", HttpStatus.BAD_REQUEST),
    CANNOT_REMOVE_LEADER(2005, "Không thể loại bỏ người quản lý dự án", HttpStatus.BAD_REQUEST),
    MANAGER_MUST_TRANSFER_BEFORE_LEAVING(2006, "Người quản lý phải chuyển quyền trước khi rời khỏi dự án", HttpStatus.BAD_REQUEST),
    TASK_NOT_EXISTED(3001, "Task không tồn tại", HttpStatus.NOT_FOUND),
    INVALID_DEADLINE(4001, "Deadline không hợp lệ", HttpStatus.BAD_REQUEST),
    ASSIGNMENT_NOT_EXISTED(4002, "Assignment không tồn tại", HttpStatus.NOT_FOUND),
    INVALID_ASSIGNMENT(4003, "Giao việc không hợp lệ", HttpStatus.BAD_REQUEST),
    TEAM_NOT_EXISTED(2007, "Nhóm không tồn tại", HttpStatus.NOT_FOUND),
    USER_NOT_IN_TEAM(4004, "Người dùng không phải là thành viên của nhóm này", HttpStatus.BAD_REQUEST),
    TEAM_NOT_STARTED(4005, "Nhóm chưa bắt đầu", HttpStatus.BAD_REQUEST),
    LEADER_CANNOT_LEAVE(4006, "Leader không thể tự rời nhóm, phải chỉ định leader mới trước", HttpStatus.BAD_REQUEST),
    PROJECT_TIME_INVALID(2001, "Thời hạn của dự án không hợp lệ", HttpStatus.BAD_REQUEST);

    private final int code;
    private final String message;
    private final HttpStatusCode statusCode;

    ErrorCode(int code, String message, HttpStatusCode statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }
}