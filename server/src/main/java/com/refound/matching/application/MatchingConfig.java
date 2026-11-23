package com.refound.matching.application;

import com.refound.matching.domain.MatchWeights;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Tạo {@link MatchWeights} lúc khởi động: tham số sai thì server không lên (04 §4.2 "lỗi cấu hình khi khởi động"). */
@Configuration
public class MatchingConfig {

    @Bean
    MatchWeights matchWeights() {
        return MatchWeights.DEFAULT;
    }
}
