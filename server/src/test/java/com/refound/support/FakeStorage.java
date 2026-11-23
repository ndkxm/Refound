package com.refound.support;

import com.refound.common.storage.StorageClient;
import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.ResourceAccessException;

/** Storage giả trong bộ nhớ (09 §9.2): test không gọi Supabase thật. URL ký có dạng {@code https://storage.test/<path>}. */
@TestComponent
@Primary
public class FakeStorage implements StorageClient {

    private final Map<String, byte[]> files = new ConcurrentHashMap<>();

    /** Bật để giả Storage sập (7.3); test nào bật phải tắt lại trong {@code finally}. */
    public volatile boolean failUploads;

    @Override
    public void upload(String path, byte[] bytes, String contentType) {
        if (failUploads) {
            throw new ResourceAccessException("Storage giả đang lỗi");
        }
        files.put(path, bytes);
    }

    @Override
    public Map<String, String> signedUrls(Collection<String> paths, Duration ttl) {
        return paths.stream().filter(files::containsKey)
                .collect(Collectors.toMap(p -> p, p -> "https://storage.test/" + p));
    }

    public byte[] file(String path) {
        return files.get(path);
    }
}
