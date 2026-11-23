package com.refound.common.storage;

import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Storage API của Supabase bằng khóa secret của server. Đã gọi thật ngày 23/11/2026 (U12):
 * upload với {@code x-upsert}, ký theo lô trả {@code signedURL} tương đối từ {@code /storage/v1}.
 */
@Component
public class SupabaseStorageClient implements StorageClient {

    private final RestClient http;
    private final String baseUrl;
    private final String bucket;

    public SupabaseStorageClient(
            RestClient.Builder builder,
            @Value("${refound.storage.url}") String url,
            @Value("${refound.storage.key}") String key,
            @Value("${refound.storage.bucket}") String bucket) {
        this.baseUrl = url + "/storage/v1";
        this.bucket = bucket;
        this.http = builder.baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + key)
                .defaultHeader("apikey", key)
                .build();
    }

    @Override
    public void upload(String path, byte[] bytes, String contentType) {
        http.post()
                .uri("/object/{bucket}/{path}", bucket, path)
                .header("x-upsert", "true")
                .contentType(MediaType.parseMediaType(contentType))
                .body(bytes)
                .retrieve()
                .toBodilessEntity();
    }

    @Override
    public byte[] download(String path) {
        byte[] bytes = http.get().uri("/object/{bucket}/{path}", bucket, path).retrieve().body(byte[].class);
        return bytes == null ? new byte[0] : bytes;
    }

    @Override
    public Map<String, String> signedUrls(Collection<String> paths, Duration ttl) {
        if (paths.isEmpty()) {
            return Map.of();
        }
        List<SignResult> results = http.post()
                .uri("/object/sign/{bucket}", bucket)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("expiresIn", ttl.toSeconds(), "paths", paths))
                .retrieve()
                .body(new ParameterizedTypeReference<List<SignResult>>() {});
        Map<String, String> urls = new HashMap<>();
        if (results != null) {
            for (SignResult r : results) {
                if (r.signedURL() != null) {
                    urls.put(r.path(), baseUrl + r.signedURL());
                }
            }
        }
        return urls;
    }

    record SignResult(String path, String signedURL, String error) {}
}
