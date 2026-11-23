package com.refound.support;

import com.refound.matching.infrastructure.VisionClient;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.context.annotation.Primary;

/** Groq giả: trả {@link #caption} (rỗng = giả Groq lỗi), ghi lại ảnh đã nhận. Test nào đổi phải gọi {@link #reset()}. */
@TestComponent
@Primary
public class FakeVisionClient implements VisionClient {

    public static final String DEFAULT_CAPTION = "Ví da màu nâu, gập đôi, có logo kim loại";

    public final List<List<byte[]>> calls = new CopyOnWriteArrayList<>();
    public volatile Optional<String> caption = Optional.of(DEFAULT_CAPTION);

    @Override
    public Optional<String> describe(List<byte[]> jpegs) {
        calls.add(List.copyOf(jpegs));
        return caption;
    }

    public void reset() {
        calls.clear();
        caption = Optional.of(DEFAULT_CAPTION);
    }
}
