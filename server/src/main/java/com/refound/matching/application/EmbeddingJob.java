package com.refound.matching.application;

import com.refound.common.storage.StorageClient;
import com.refound.item.application.ItemContentChanged;
import com.refound.matching.domain.EmbeddingBackoff;
import com.refound.matching.domain.EmbeddingText;
import com.refound.matching.infrastructure.EmbeddingClient;
import com.refound.matching.infrastructure.EmbeddingClient.EmbeddingException;
import com.refound.matching.infrastructure.EmbeddingClient.Input;
import com.refound.matching.infrastructure.EmbeddingRepository;
import com.refound.matching.infrastructure.EmbeddingRepository.Thumb;
import com.refound.matching.infrastructure.EmbeddingRepository.Vector;
import com.refound.matching.infrastructure.EmbeddingRepository.Work;
import com.refound.matching.infrastructure.VisionClient;
import jakarta.annotation.PreDestroy;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClientException;

/**
 * Job embedding (04 §4.1, 4.4, ADR-01): chạy ngay sau khi tin được ghi (sự kiện sau commit) và vét mỗi 30 giây, kể
 * cả lần đầu lúc khởi động (server vừa thức sau khi Render cho ngủ). Mọi lượt chạy trên một luồng riêng; đang chạy
 * mà có thêm yêu cầu thì giữ đúng một lượt chờ, các yêu cầu sau bỏ (lượt chờ sẽ nhặt luôn việc của chúng).
 *
 * <p>Mỗi tin: mô tả ảnh bằng Groq (nếu chưa có, có ảnh, không nhạy cảm) → embed chữ + từng ảnh bằng Gemini trong
 * một lô → ghi vector. Tin danh mục nhạy cảm không gửi ảnh cho AI nào (BR-08).
 */
@Component
public class EmbeddingJob {

    static final int BATCH = 5; // 4.4
    static final Duration LEASE = Duration.ofMinutes(5);
    private static final Logger log = LoggerFactory.getLogger(EmbeddingJob.class);

    private final EmbeddingRepository repo;
    private final StorageClient storage;
    private final EmbeddingClient embeddings;
    private final VisionClient vision;
    private final AiCircuit circuit;
    private final TransactionTemplate tx;
    private final Clock clock;
    private final boolean enabled;
    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(1), r -> {
                Thread t = new Thread(r, "embedding-job");
                t.setDaemon(true);
                return t;
            }, new ThreadPoolExecutor.DiscardPolicy());

    public EmbeddingJob(EmbeddingRepository repo, StorageClient storage, EmbeddingClient embeddings,
            VisionClient vision, AiCircuit circuit, TransactionTemplate tx, Clock clock,
            @Value("${refound.embedding.enabled:true}") boolean enabled) {
        this.repo = repo;
        this.storage = storage;
        this.embeddings = embeddings;
        this.vision = vision;
        this.circuit = circuit;
        this.tx = tx;
        this.clock = clock;
        this.enabled = enabled;
    }

    @TransactionalEventListener
    public void onContentChanged(ItemContentChanged event) {
        kick();
    }

    @Scheduled(initialDelay = 0, fixedDelayString = "${refound.embedding.poll-ms:30000}")
    public void sweep() {
        kick();
    }

    /** Xếp một lượt chạy trên luồng của job; tắt bằng {@code refound.embedding.enabled=false} (test gọi tay). */
    public void kick() {
        if (enabled) {
            executor.execute(this::drain);
        }
    }

    private void drain() {
        try {
            while (runOnce() == BATCH) {
                // còn việc đến hạn: làm tiếp lô sau
            }
        } catch (RuntimeException e) { // DB lỗi: lượt vét sau thử lại, không để luồng chết im lặng
            log.error("Job embedding lỗi", e);
        }
    }

    /** Một lô: nhận tối đa {@value #BATCH} tin đến hạn và xử lý lần lượt. @return số tin đã nhận */
    public int runOnce() {
        if (circuit.isOpen()) {
            return 0;
        }
        Instant now = clock.instant();
        List<Work> batch = repo.claimDue(now, now.plus(LEASE), BATCH);
        for (Work w : batch) {
            process(w);
        }
        return batch.size();
    }

    private void process(Work w) {
        if (circuit.isOpen()) { // một tin trước trong lô vừa gặp 429
            repo.release(w.itemId(), circuit.pausedUntil());
            return;
        }
        String newCaption = null;
        try {
            List<Thumb> thumbs = w.sensitive() ? List.of() : w.thumbs(); // BR-08
            List<byte[]> images = new ArrayList<>();
            for (Thumb t : thumbs) {
                images.add(storage.download(t.path()));
            }
            if (w.aiCaption() == null && !images.isEmpty()) {
                newCaption = vision.describe(images.subList(0, Math.min(VisionClient.MAX_IMAGES, images.size())))
                        .orElse(null);
            }
            String caption = newCaption != null ? newCaption : (w.sensitive() ? null : w.aiCaption());
            List<Input> inputs = new ArrayList<>();
            inputs.add(new Input.Text(EmbeddingText.forItem(w.categoryName(), w.description(), caption)));
            images.forEach(img -> inputs.add(new Input.Image(img)));

            List<float[]> vectors = embeddings.embed(inputs);
            List<Vector> rows = new ArrayList<>();
            rows.add(new Vector("text", null, vectors.get(0)));
            for (int i = 0; i < thumbs.size(); i++) {
                rows.add(new Vector("image", thumbs.get(i).imageId(), vectors.get(i + 1)));
            }
            String captionToSave = newCaption;
            Boolean saved = tx.execute(s -> repo.saveDone(w, captionToSave, embeddings.model(), rows));
            if (!Boolean.TRUE.equals(saved)) {
                log.info("Tin {} đổi trong lúc embed, bỏ kết quả cũ", w.itemId());
            }
        } catch (EmbeddingException e) {
            if (e.rateLimited()) {
                circuit.pause();
            }
            fail(w, newCaption, e);
        } catch (RestClientException e) { // không đọc được ảnh từ Storage
            fail(w, newCaption, e);
        }
    }

    private void fail(Work w, String newCaption, Exception e) {
        int attempts = w.attempts() + 1;
        Optional<Duration> delay = EmbeddingBackoff.retryDelay(attempts);
        Instant next = clock.instant().plus(delay.orElse(Duration.ZERO));
        repo.saveFailure(w, attempts, delay.isEmpty(), next, newCaption);
        log.warn("Embedding tin {} lỗi lần {}{}: {}", w.itemId(), attempts,
                delay.map(d -> ", thử lại sau " + d.toMinutes() + " phút").orElse(", dừng (failed)"), e.getMessage());
    }

    @PreDestroy
    void stop() {
        executor.shutdownNow();
    }
}
