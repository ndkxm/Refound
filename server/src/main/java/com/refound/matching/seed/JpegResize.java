package com.refound.matching.seed;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import javax.imageio.ImageIO;

/**
 * Thu ảnh mẫu về cạnh dài 1024 (ảnh đầy đủ) / 512 (ảnh nhỏ, ảnh gửi embed) như app làm trước khi upload (ADR-14).
 * shortcut: không đọc EXIF xoay ảnh, ảnh chụp dọc phải được xoay sẵn khi xuất; đủ cho bộ mẫu do nhóm tự chuẩn bị.
 */
public final class JpegResize {

    private JpegResize() {}

    public static byte[] maxSide(byte[] jpeg, int maxSide) {
        try {
            BufferedImage src = ImageIO.read(new ByteArrayInputStream(jpeg));
            if (src == null) {
                throw new IllegalArgumentException("Không phải ảnh đọc được");
            }
            double scale = Math.min(1.0, (double) maxSide / Math.max(src.getWidth(), src.getHeight()));
            int w = Math.max(1, (int) Math.round(src.getWidth() * scale));
            int h = Math.max(1, (int) Math.round(src.getHeight() * scale));
            BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = out.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(src, 0, 0, w, h, null);
            g.dispose();
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            ImageIO.write(out, "jpg", bytes);
            return bytes.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
