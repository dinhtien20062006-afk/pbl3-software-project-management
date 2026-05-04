package com.pbl3.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(value = AppException.class)
    public ResponseEntity<?> handlingAppException(AppException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        return ResponseEntity
                .status(errorCode.getStatusCode())
                .body(Map.of(
                    "code", errorCode.getCode(),
                    "message", errorCode.getMessage()
                ));
    }

    @ExceptionHandler(value = Exception.class)
    public ResponseEntity<?> handlingRuntimeException(Exception exception) {
        return ResponseEntity.badRequest().body(Map.of(
            "code", 9999,
            "message", "Lỗi hệ thống: " + exception.getMessage()
        ));
    }
}