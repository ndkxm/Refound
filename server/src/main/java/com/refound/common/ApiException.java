package com.refound.common;

import org.springframework.http.HttpStatus;

/** Lỗi nghiệp vụ trả về app dạng ProblemDetail: {@code code} máy đọc, {@code detail} tiếng Việt (06 §6.1). */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public ApiException(HttpStatus status, String code, String detail) {
        super(detail);
        this.status = status;
        this.code = code;
    }

    public static ApiException unauthenticated(String detail) {
        return new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", detail);
    }

    public static ApiException notFound(String detail) {
        return new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", detail);
    }

    public static ApiException validation(String detail) {
        return new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", detail);
    }

    public HttpStatus status() {
        return status;
    }

    public String code() {
        return code;
    }
}
