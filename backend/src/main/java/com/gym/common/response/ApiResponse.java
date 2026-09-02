package com.gym.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 모든 API 응답의 통일 래퍼 (CLAUDE.md §5).
 * 성공: { "success": true, "data": ... }
 * 실패: { "success": false, "error": { "code": ..., "message": ... } }
 * null 필드는 직렬화에서 제외한다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(boolean success, T data, ErrorBody error) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static ApiResponse<Void> error(String code, String message) {
        return new ApiResponse<>(false, null, new ErrorBody(code, message));
    }

    public record ErrorBody(String code, String message) {
    }
}
