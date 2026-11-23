package com.refound.auth.presentation;

import com.refound.auth.application.AuthService;
import com.refound.auth.application.AuthService.LoginResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    /** FR-AUTH-01: lần đầu đăng nhập là đăng ký; {@code isNew=true} thì app mở màn đặt tên hiển thị. */
    @PostMapping("/firebase")
    public LoginResult firebase(@Valid @RequestBody LoginRequest req) {
        return auth.login(req.idToken(), req.displayName());
    }

    public record LoginRequest(
            @NotBlank(message = "Thiếu idToken") String idToken,
            @Size(min = 2, max = 40, message = "Tên hiển thị dài 2–40 ký tự") String displayName) {

        public LoginRequest {
            displayName = displayName == null ? null : displayName.strip();
        }
    }
}
