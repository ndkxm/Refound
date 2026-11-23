package com.refound.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.refound.auth.application.JwtIssuer;
import com.refound.auth.infrastructure.UserRepository;
import com.refound.auth.presentation.UserStatusFilter;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

/**
 * Chuỗi bảo mật (03 §3.3): JWT HS256 của server cho mọi API trừ {@code /auth/**} và {@code /health};
 * {@code /admin/**} cần ADMIN; sau đó {@link UserStatusFilter} kiểm tài khoản còn {@code active}.
 */
@Configuration
public class SecurityConfig {

    private static final String AUTH_PATH = "/api/v1/auth/";

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtIssuer jwt, UserRepository users, ObjectMapper json)
            throws Exception {
        AuthenticationEntryPoint unauthenticated = (req, res, e) -> ApiErrors.write(
                res, json, HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại.");
        AccessDeniedHandler forbidden = (req, res, e) -> ApiErrors.write(
                res, json, HttpStatus.FORBIDDEN, "FORBIDDEN", "Bạn không có quyền thực hiện thao tác này.");

        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(b -> b.disable())
                .formLogin(f -> f.disable())
                .authorizeHttpRequests(a -> a
                        .requestMatchers(HttpMethod.GET, "/api/v1/health").permitAll()
                        .requestMatchers(AUTH_PATH + "**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(o -> o
                        .bearerTokenResolver(ignoreOnAuthPaths())
                        .authenticationEntryPoint(unauthenticated)
                        .accessDeniedHandler(forbidden)
                        .jwt(j -> j.decoder(jwt.decoder()).jwtAuthenticationConverter(token -> new JwtAuthenticationToken(
                                token,
                                List.of(new SimpleGrantedAuthority("ROLE_" + String.valueOf(token.getClaimAsString("role")).toUpperCase())),
                                token.getSubject()))))
                .exceptionHandling(e -> e.authenticationEntryPoint(unauthenticated).accessDeniedHandler(forbidden))
                .addFilterAfter(new UserStatusFilter(users, json), BearerTokenAuthenticationFilter.class)
                .build();
    }

    /** App có thể còn gửi JWT cũ đã hết hạn khi đăng nhập lại; ở {@code /auth/**} bỏ qua header để khỏi bị 401. */
    private static BearerTokenResolver ignoreOnAuthPaths() {
        DefaultBearerTokenResolver delegate = new DefaultBearerTokenResolver();
        return req -> req.getRequestURI().startsWith(AUTH_PATH) ? null : delegate.resolve(req);
    }
}
