package com.refound.auth.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.refound.auth.infrastructure.UserRepository;
import com.refound.auth.infrastructure.UserRepository.StatusAndRole;
import com.refound.common.ApiErrors;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Sau khi JWT hợp lệ: một truy vấn theo khóa chính, tài khoản không còn {@code active} thì 401 dù JWT còn hạn (X19).
 * Quyền lấy từ DB thay cho claim {@code role} trong JWT, để khóa hoặc hạ quyền admin có tác dụng ngay.
 * Không đánh dấu {@code @Component}: nếu là bean, Spring Boot tự gắn nó vào mọi request ngoài chuỗi bảo mật.
 */
public class UserStatusFilter extends OncePerRequestFilter {

    private final UserRepository users;
    private final ObjectMapper json;

    public UserStatusFilter(UserRepository users, ObjectMapper json) {
        this.users = users;
        this.json = json;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken token) {
            Optional<StatusAndRole> current = userId(token).flatMap(users::findStatusAndRole);
            if (current.isEmpty() || !"active".equals(current.get().status())) {
                SecurityContextHolder.clearContext();
                ApiErrors.write(res, json, HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED",
                        "Tài khoản đã bị khóa hoặc không còn tồn tại.");
                return;
            }
            String role = "ROLE_" + current.get().role().toUpperCase();
            SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(
                    token.getToken(), List.of(new SimpleGrantedAuthority(role)), token.getName()));
        }
        chain.doFilter(req, res);
    }

    private static Optional<UUID> userId(JwtAuthenticationToken token) {
        try {
            return Optional.of(UUID.fromString(token.getToken().getSubject()));
        } catch (IllegalArgumentException | NullPointerException e) {
            return Optional.empty();
        }
    }
}
