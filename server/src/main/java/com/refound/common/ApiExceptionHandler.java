package com.refound.common;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/** Mọi lỗi trả ProblemDetail + {@code code} (bảng mã ở 06 §6.1). */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ProblemDetail> handleApi(ApiException e) {
        return ResponseEntity.status(e.status()).body(ApiErrors.problem(e.status(), e.code(), e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> handleUnexpected(Exception e) {
        log.error("Lỗi không lường trước", e);
        HttpStatus s = HttpStatus.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(s).body(ApiErrors.problem(s, "INTERNAL", "Máy chủ gặp lỗi, vui lòng thử lại."));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException e, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail p = ApiErrors.problem(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Dữ liệu gửi lên không hợp lệ.");
        List<Map<String, String>> errors = e.getBindingResult().getFieldErrors().stream()
                .map(f -> Map.of("field", f.getField(), "message", String.valueOf(f.getDefaultMessage())))
                .toList();
        p.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(p);
    }

    @Override
    protected ResponseEntity<Object> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException e, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        HttpStatus s = HttpStatus.PAYLOAD_TOO_LARGE;
        return ResponseEntity.status(s).body(ApiErrors.problem(s, "PAYLOAD_TOO_LARGE", "Ảnh quá lớn (tối đa 1 MB)."));
    }

    /** Lỗi chuẩn của Spring MVC (sai đường dẫn, sai method, JSON hỏng…): thêm {@code code} theo mã HTTP. */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception e, Object body, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ResponseEntity<Object> res = super.handleExceptionInternal(e, body, headers, status, request);
        if (res != null && res.getBody() instanceof ProblemDetail p
                && (p.getProperties() == null || !p.getProperties().containsKey("code"))) {
            p.setProperty("code", switch (status.value()) {
                case 400 -> "VALIDATION_FAILED";
                case 404 -> "NOT_FOUND";
                case 413 -> "PAYLOAD_TOO_LARGE";
                default -> "HTTP_" + status.value();
            });
        }
        return res;
    }
}
