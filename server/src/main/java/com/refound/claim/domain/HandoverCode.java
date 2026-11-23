package com.refound.claim.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Mã bàn giao QR (C5b, C6): ngẫu nhiên 128 bit, DB chỉ giữ sha256 ({@code handovers.code_hash}), hạn 10 phút.
 * Dùng một lần: quét thành công thì claim {@code completed}, máy trạng thái chặn lần quét sau; tạo mã mới thì
 * {@code code_hash} bị ghi đè nên mã cũ hết hiệu lực.
 */
public final class HandoverCode {

    public static final Duration TTL = Duration.ofMinutes(10);
    private static final int BYTES = 16;

    private HandoverCode() {}

    /** {@code code} chỉ trả cho Finder một lần để vẽ QR; {@code hash} và {@code expiresAt} ghi vào {@code handovers}. */
    public record Issued(String code, String hash, Instant expiresAt) {}

    public static Issued issue(SecureRandom random, Clock clock) {
        byte[] bytes = new byte[BYTES];
        random.nextBytes(bytes);
        String code = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        return new Issued(code, hash(code), clock.instant().plus(TTL));
    }

    public static String hash(String code) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(code.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("JVM không có SHA-256", e);
        }
    }

    /**
     * Mã khớp và còn hạn. So hash bằng {@link MessageDigest#isEqual} (thời gian không phụ thuộc vị trí byte sai).
     * Hết hạn tính cả đúng thời điểm {@code expiresAt}.
     */
    public static boolean verify(String code, String storedHash, Instant expiresAt, Clock clock) {
        if (code == null || code.isBlank() || storedHash == null || expiresAt == null) {
            return false;
        }
        if (!clock.instant().isBefore(expiresAt)) {
            return false;
        }
        return MessageDigest.isEqual(
                hash(code).getBytes(StandardCharsets.US_ASCII), storedHash.getBytes(StandardCharsets.US_ASCII));
    }
}
