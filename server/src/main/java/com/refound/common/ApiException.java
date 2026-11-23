package com.refound.common;

import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;

/** Lỗi nghiệp vụ trả về app dạng ProblemDetail: {@code code} máy đọc, {@code detail} tiếng Việt (06 §6.1). */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;
    private final Map<String, Object> properties;

    public ApiException(HttpStatus status, String code, String detail) {
        this(status, code, detail, Map.of());
    }

    /** {@code properties} thêm vào ProblemDetail, ví dụ {@code errors}, {@code retryAfterSeconds} (06 §6.1). */
    public ApiException(HttpStatus status, String code, String detail, Map<String, Object> properties) {
        super(detail);
        this.status = status;
        this.code = code;
        this.properties = Map.copyOf(properties);
    }

    public static ApiException unauthenticated(String detail) {
        return new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", detail);
    }

    public static ApiException forbidden(String detail) {
        return new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", detail);
    }

    public static ApiException notFound(String detail) {
        return new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", detail);
    }

    public static ApiException validation(String detail) {
        return new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", detail);
    }

    /** 400 kèm {@code errors: [{field, message}]}, cùng dạng lỗi {@code @Valid} của {@link ApiExceptionHandler}. */
    public static ApiException validation(List<FieldError> errors) {
        return new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Dữ liệu gửi lên không hợp lệ.",
                Map.of("errors", errors.stream().map(e -> Map.of("field", e.field(), "message", e.message())).toList()));
    }

    public static ApiException invalidState(String detail) {
        return new ApiException(HttpStatus.CONFLICT, "INVALID_STATE", detail);
    }

    public HttpStatus status() {
        return status;
    }

    public String code() {
        return code;
    }

    public Map<String, Object> properties() {
        return properties;
    }

    public record FieldError(String field, String message) {}
}
