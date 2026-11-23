package com.refound.matching.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Component;

/**
 * Gemini trả 429 (hết hạn mức phút/ngày) thì tạm dừng mọi lời gọi 5 phút (04 §4.1, 7.3): job embedding và
 * {@code /search} đều hỏi đây trước khi gọi. Một instance nên biến trong bộ nhớ là đủ; server khởi động lại thì
 * quên, cùng lắm gọi thêm một lần và nhận 429 lại.
 */
@Component
public class AiCircuit {

    static final Duration PAUSE = Duration.ofMinutes(5);

    private final Clock clock;
    private volatile Instant pausedUntil = Instant.MIN;

    public AiCircuit(Clock clock) {
        this.clock = clock;
    }

    public boolean isOpen() {
        return clock.instant().isBefore(pausedUntil);
    }

    public Instant pausedUntil() {
        return pausedUntil;
    }

    public void pause() {
        pausedUntil = clock.instant().plus(PAUSE);
    }

    /** Cho test. */
    public void reset() {
        pausedUntil = Instant.MIN;
    }
}
