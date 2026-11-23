package com.refound.matching.application;

import com.refound.matching.domain.MatchWeights;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Tạo {@link MatchWeights} lúc khởi động: tham số sai thì server không lên (04 §4.2 "lỗi cấu hình khi khởi động").
 * Bật {@code @Scheduled} cho job embedding (vét mỗi 30 giây, 4.4).
 */
@Configuration
@EnableScheduling
public class MatchingConfig {

    @Bean
    MatchWeights matchWeights() {
        return MatchWeights.DEFAULT;
    }
}
