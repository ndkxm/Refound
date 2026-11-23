package com.refound.auth.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Hàm thuần của AuthService và ProfileService. */
class AuthServiceTest {

    @Test
    void requestedNameWins() {
        assertThat(AuthService.initialName("Kiên", "Nguyễn Kiên")).isEqualTo("Kiên");
    }

    @Test
    void googleNameUsedAndStripped() {
        assertThat(AuthService.initialName(null, "  Nguyễn Kiên  ")).isEqualTo("Nguyễn Kiên");
    }

    @Test
    void googleNameCutTo40Characters() {
        String name = "a".repeat(39) + " bcd";
        assertThat(AuthService.initialName(null, name)).isEqualTo("a".repeat(39));
    }

    @Test
    void missingOrTooShortNameFallsBack() {
        assertThat(AuthService.initialName(null, null)).isEqualTo("Người dùng");
        assertThat(AuthService.initialName(null, " K ")).isEqualTo("Người dùng");
    }

    @Test
    void jpegDetectedByMagicBytes() {
        assertThat(ProfileService.isJpeg(new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00})).isTrue();
        assertThat(ProfileService.isJpeg("\u0089PNG".getBytes(java.nio.charset.StandardCharsets.ISO_8859_1))).isFalse();
        assertThat(ProfileService.isJpeg(new byte[] {(byte) 0xFF, (byte) 0xD8})).isFalse();
    }
}
