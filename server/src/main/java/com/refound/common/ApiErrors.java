package com.refound.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;

/** Dựng ProblemDetail có {@code code}; dùng ở controller advice lẫn filter (filter chạy trước advice). */
public final class ApiErrors {

    private ApiErrors() {}

    public static ProblemDetail problem(HttpStatus status, String code, String detail) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(status, detail);
        p.setProperty("code", code);
        return p;
    }

    public static void write(HttpServletResponse res, ObjectMapper json, HttpStatus status, String code, String detail)
            throws IOException {
        res.setStatus(status.value());
        res.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        res.setCharacterEncoding("UTF-8");
        json.writeValue(res.getOutputStream(), problem(status, code, detail));
    }
}
