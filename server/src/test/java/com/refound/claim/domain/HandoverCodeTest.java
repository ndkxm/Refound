package com.refound.claim.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.refound.claim.domain.HandoverCode.Issued;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Sinh, băm, so sánh và hết hạn của mã QR bàn giao với Clock giả (08 §8.2). */
class HandoverCodeTest {

    static final Instant T0 = Instant.parse("2026-11-20T02:00:00Z");
    static final Clock AT_T0 = Clock.fixed(T0, ZoneOffset.UTC);

    @Test
    void issuedCodeIsRandomUrlSafeAndExpiresIn10Minutes() {
        Issued issued = HandoverCode.issue(new SecureRandom(), AT_T0);
        assertThat(issued.code()).matches("[A-Za-z0-9_-]{22}"); // 128 bit, base64url không đệm
        assertThat(issued.hash()).isEqualTo(HandoverCode.hash(issued.code())).hasSize(64).doesNotContain(issued.code());
        assertThat(issued.expiresAt()).isEqualTo(T0.plus(Duration.ofMinutes(10)));

        Set<String> codes = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            codes.add(HandoverCode.issue(new SecureRandom(), AT_T0).code());
        }
        assertThat(codes).hasSize(1000);
    }

    /** Đáp án tính sẵn: sha256("abc") theo FIPS 180-2. */
    @Test
    void hashIsSha256Hex() {
        assertThat(HandoverCode.hash("abc"))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }

    @Test
    void verifyMatchesOnlyBeforeExpiry() {
        Issued issued = HandoverCode.issue(new SecureRandom(), AT_T0);
        assertThat(HandoverCode.verify(issued.code(), issued.hash(), issued.expiresAt(), at(T0))).isTrue();
        assertThat(HandoverCode.verify(issued.code(), issued.hash(), issued.expiresAt(),
                at(T0.plus(Duration.ofMinutes(10)).minusMillis(1)))).isTrue();
        // đúng thời điểm hết hạn đã coi là hết hạn
        assertThat(HandoverCode.verify(issued.code(), issued.hash(), issued.expiresAt(),
                at(T0.plus(Duration.ofMinutes(10))))).isFalse();
    }

    @Test
    void verifyRejectsWrongOrMissingInput() {
        Issued issued = HandoverCode.issue(new SecureRandom(), AT_T0);
        Issued other = HandoverCode.issue(new SecureRandom(), AT_T0); // mã cũ sau khi Finder tạo mã mới
        assertThat(HandoverCode.verify(other.code(), issued.hash(), issued.expiresAt(), AT_T0)).isFalse();
        assertThat(HandoverCode.verify(null, issued.hash(), issued.expiresAt(), AT_T0)).isFalse();
        assertThat(HandoverCode.verify("  ", issued.hash(), issued.expiresAt(), AT_T0)).isFalse();
        assertThat(HandoverCode.verify(issued.code(), null, issued.expiresAt(), AT_T0)).isFalse(); // chưa tạo mã
        assertThat(HandoverCode.verify(issued.code(), issued.hash(), null, AT_T0)).isFalse();
        // gửi thẳng hash thay cho mã không qua được
        assertThat(HandoverCode.verify(issued.hash(), issued.hash(), issued.expiresAt(), AT_T0)).isFalse();
    }

    private static Clock at(Instant t) {
        return Clock.fixed(t, ZoneOffset.UTC);
    }
}
