package com.refound.common.storage;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;

/** Kho ảnh private (ADR-06). Test thay bằng bản giả, không gọi Supabase thật (09 §9.2). */
public interface StorageClient {

    /** Ghi đè nếu đã có file ở {@code path}. */
    void upload(String path, byte[] bytes, String contentType);

    /** Đọc một file bằng khóa của server (job embedding đọc ảnh nhỏ để gửi AI). Lỗi là {@code RestClientException}. */
    byte[] download(String path);

    /** Ký nhiều đường dẫn trong một lần gọi (U12). Đường dẫn không tồn tại thì không có trong kết quả. */
    Map<String, String> signedUrls(Collection<String> paths, Duration ttl);
}
