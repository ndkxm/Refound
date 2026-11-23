package com.refound.common;

/** Nhận diện JPEG bằng byte đầu ({@code FF D8 FF}); không tin {@code Content-Type} do app gửi. */
public final class Jpeg {

    private Jpeg() {}

    public static boolean isJpeg(byte[] b) {
        return b != null && b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF;
    }
}
