package com.refound.auth.presentation;

import com.refound.auth.application.MeDto;
import com.refound.auth.application.ProfileService;
import com.refound.common.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Hồ sơ của chính người gọi (FR-AUTH-02). userId lấy từ JWT, không nhận từ body. */
@RestController
@RequestMapping("/api/v1/me")
public class MeController {

    private final ProfileService profiles;

    public MeController(ProfileService profiles) {
        this.profiles = profiles;
    }

    @GetMapping
    public MeDto me(@AuthenticationPrincipal Jwt jwt) {
        return profiles.me(CurrentUser.id(jwt));
    }

    @PatchMapping
    public MeDto update(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UpdateMeRequest req) {
        return profiles.rename(CurrentUser.id(jwt), req.displayName());
    }

    /** App đã resize về JPEG 512px (ADR-06); server chỉ kiểm định dạng và dung lượng. */
    @PutMapping(path = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public MeDto avatar(@AuthenticationPrincipal Jwt jwt, @RequestPart("file") MultipartFile file) throws IOException {
        return profiles.changeAvatar(CurrentUser.id(jwt), file.getBytes());
    }

    public record UpdateMeRequest(@Size(min = 2, max = 40, message = "Tên hiển thị dài 2–40 ký tự") String displayName) {

        public UpdateMeRequest {
            displayName = displayName == null ? null : displayName.strip();
        }
    }
}
