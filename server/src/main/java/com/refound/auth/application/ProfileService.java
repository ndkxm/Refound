package com.refound.auth.application;

import com.refound.auth.domain.User;
import com.refound.auth.infrastructure.UserRepository;
import com.refound.common.ApiException;
import com.refound.common.Jpeg;
import com.refound.common.storage.StorageClient;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

/** Hồ sơ: xem, đổi tên, đổi ảnh đại diện (FR-AUTH-02). */
@Service
public class ProfileService {

    /** Chỉ có đăng nhập Google (D10). */
    static final String AUTH_PROVIDER = "google";
    static final long MAX_AVATAR_BYTES = 1024 * 1024; // ADR-06
    private static final Duration URL_TTL = Duration.ofHours(1); // ADR-06
    private static final Logger log = LoggerFactory.getLogger(ProfileService.class);

    private final UserRepository users;
    private final StorageClient storage;

    public ProfileService(UserRepository users, StorageClient storage) {
        this.users = users;
        this.storage = storage;
    }

    public MeDto me(UUID userId) {
        return toMe(load(userId));
    }

    public MeDto rename(UUID userId, String displayName) {
        if (displayName != null) {
            users.updateDisplayName(userId, displayName);
        }
        return me(userId);
    }

    public MeDto changeAvatar(UUID userId, byte[] jpeg) {
        if (jpeg.length > MAX_AVATAR_BYTES) { // Tomcat chặn trước ở max-file-size; đây là lớp thứ hai
            throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "PAYLOAD_TOO_LARGE", "Ảnh quá lớn (tối đa 1 MB).");
        }
        if (!Jpeg.isJpeg(jpeg)) {
            throw ApiException.validation("Ảnh đại diện phải là ảnh JPEG.");
        }
        String path = "avatars/" + userId + ".jpg"; // ghi đè ảnh cũ, không để lại file rác
        storage.upload(path, jpeg, "image/jpeg");
        users.updateAvatarPath(userId, path);
        return me(userId);
    }

    public UserPublicDto publicProfile(UUID userId) {
        User u = users.findById(userId).orElseThrow(() -> ApiException.notFound("Không tìm thấy người dùng."));
        return new UserPublicDto(u.id(), u.displayName(), avatarUrl(u.avatarPath()), u.reputation());
    }

    MeDto toMe(User u) {
        return new MeDto(u.id(), u.displayName(), avatarUrl(u.avatarPath()), u.role(), u.reputation(),
                AUTH_PROVIDER, u.notificationPrefs());
    }

    private User load(UUID userId) {
        // UserStatusFilter đã chặn user không tồn tại; ở đây chỉ phòng trường hợp bị xóa giữa chừng.
        return users.findById(userId).orElseThrow(() -> ApiException.unauthenticated("Tài khoản không còn tồn tại."));
    }

    /** Ảnh đại diện không quan trọng bằng dữ liệu: Storage lỗi thì trả null thay vì làm hỏng cả request. */
    private String avatarUrl(String path) {
        if (path == null) {
            return null;
        }
        try {
            return storage.signedUrls(List.of(path), URL_TTL).get(path);
        } catch (RestClientException e) {
            log.warn("Không ký được URL ảnh đại diện {}", path, e);
            return null;
        }
    }
}
