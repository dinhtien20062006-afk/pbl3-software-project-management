package com.pbl3.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL) // Bỏ qua các field null trong JSON
public class ApiResponse<T> {
    @Builder.Default
    private int code = 1000; // Mã thành công mặc định
    private String message;
    private T result; // Đây là nơi chứa ProjectStatistics, ProjectResponse, v.v.
}