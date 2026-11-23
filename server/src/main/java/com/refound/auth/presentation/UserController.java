package com.refound.auth.presentation;

import com.refound.auth.application.ProfileService;
import com.refound.auth.application.UserPublicDto;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final ProfileService profiles;

    public UserController(ProfileService profiles) {
        this.profiles = profiles;
    }

    /** Người đăng nhập bất kỳ chỉ thấy tên, ảnh, uy tín (X17). */
    @GetMapping("/{id}")
    public UserPublicDto user(@PathVariable UUID id) {
        return profiles.publicProfile(id);
    }
}
