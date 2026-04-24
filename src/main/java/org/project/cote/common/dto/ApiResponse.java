package org.project.cote.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        T data,
        ErrorResponse error
) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static <T> ApiResponse<T> fail(ErrorCode code) {
        return new ApiResponse<>(false, null, new ErrorResponse(code.name(), code.getMessage()));
    }

    public static <T> ApiResponse<T> fail(ErrorCode code, String message) {
        return new ApiResponse<>(false, null, new ErrorResponse(code.name(), message));
    }

    public record ErrorResponse(String code, String message) {}
}
