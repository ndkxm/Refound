package com.refound.support;

import com.refound.matching.infrastructure.EmbeddingClient;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.context.annotation.Primary;

/**
 * Gemini giả (09 §9.2): mỗi chữ/ảnh ra một vector đơn vị cố định sinh từ băm nội dung (hai nội dung khác nhau gần
 * như vuông góc, cosine ≈ 0), hoặc vector đặt trước bằng {@link #define}. Ghi lại mọi lần gọi. Test nào đổi trạng
 * thái phải gọi {@link #reset()}.
 */
@TestComponent
@Primary
public class FakeEmbeddingClient implements EmbeddingClient {

    public static final String MODEL = "fake-embedding";

    public final List<List<Input>> calls = new CopyOnWriteArrayList<>();
    /** Khác null thì mọi lần gọi ném lỗi này. */
    public volatile RuntimeException failWith;
    /** Chạy giữa lúc "gọi AI", ví dụ giả người dùng sửa tin trong lúc job đang chờ. */
    public volatile Runnable during;

    private final Map<String, float[]> fixed = new ConcurrentHashMap<>();

    @Override
    public String model() {
        return MODEL;
    }

    @Override
    public List<float[]> embed(List<Input> inputs) {
        calls.add(List.copyOf(inputs));
        Runnable r = during;
        if (r != null) {
            during = null;
            r.run();
        }
        if (failWith != null) {
            throw failWith;
        }
        return inputs.stream().map(in -> switch (in) {
            case Input.Text t -> fixed.getOrDefault(t.text(), random(t.text().hashCode()));
            case Input.Image i -> random(Arrays.hashCode(i.jpeg()));
        }).toList();
    }

    /** Chữ {@code text} (đúng chuỗi gửi đi, kể cả tiền tố) sẽ ra {@code vector}. */
    public void define(String text, float[] vector) {
        fixed.put(text, vector);
    }

    public void reset() {
        calls.clear();
        failWith = null;
        during = null;
        fixed.clear();
    }

    /** Vector đơn vị có cosine với {@link #axis(int) axis(0)} đúng bằng {@code cos} (dùng trục 0 và trục {@code other}). */
    public static float[] withCosine(double cos, int other) {
        float[] v = new float[DIMENSIONS];
        v[0] = (float) cos;
        v[other] = (float) Math.sqrt(1 - cos * cos);
        return v;
    }

    public static float[] axis(int i) {
        float[] v = new float[DIMENSIONS];
        v[i] = 1;
        return v;
    }

    private static float[] random(int seed) {
        Random r = new Random(seed);
        float[] v = new float[DIMENSIONS];
        double n = 0;
        for (int i = 0; i < v.length; i++) {
            v[i] = (float) r.nextGaussian();
            n += v[i] * v[i];
        }
        for (int i = 0; i < v.length; i++) {
            v[i] /= (float) Math.sqrt(n);
        }
        return v;
    }
}
