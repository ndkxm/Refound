package com.refound.matching.infrastructure;

import java.util.List;
import java.util.Optional;

/** Mô tả ảnh đồ vật thành một câu tiếng Việt để ghép vào chữ trước khi embed (ADR-02, D11). Test dùng bản giả. */
public interface VisionClient {

    int MAX_IMAGES = 3;

    /**
     * Không bao giờ ném: dịch vụ lỗi, hết hạn mức, model bị gỡ hay chưa cấu hình đều trả rỗng, vì thiếu mô tả ảnh
     * vẫn embed được (vector ảnh vẫn có).
     *
     * @param jpegs tối đa {@link #MAX_IMAGES} ảnh nhỏ 512px của một tin, không bao giờ là ảnh danh mục nhạy cảm
     */
    Optional<String> describe(List<byte[]> jpegs);
}
