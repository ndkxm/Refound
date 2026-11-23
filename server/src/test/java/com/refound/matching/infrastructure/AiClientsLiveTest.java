package com.refound.matching.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.refound.matching.domain.EmbeddingText;
import com.refound.matching.infrastructure.EmbeddingClient.Input;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Optional;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.web.client.RestClient;

/**
 * Gọi thật Gemini và Groq bằng client của server (tốn 2 request hạn mức). Chỉ chạy khi đặt
 * {@code REFOUND_LIVE_AI=true} cùng {@code GEMINI_API_KEY}, {@code GROQ_API_KEY}; CI và {@code ./mvnw test} thường bỏ qua.
 */
@EnabledIfEnvironmentVariable(named = "REFOUND_LIVE_AI", matches = "true")
class AiClientsLiveTest {

    @Test
    void geminiEmbedsTextAndImageInOneBatch() throws Exception {
        EmbeddingClient gemini = new GeminiEmbeddingClient(RestClient.builder(),
                "https://generativelanguage.googleapis.com/v1beta", System.getenv("GEMINI_API_KEY"), "gemini-embedding-2");
        List<float[]> v = gemini.embed(List.of(
                new Input.Text(EmbeddingText.forItem("Ví", "ví da nâu gập đôi", null)), new Input.Image(jpeg())));
        assertThat(v).hasSize(2).allSatisfy(x -> {
            assertThat(x).hasSize(EmbeddingClient.DIMENSIONS);
            double n = 0;
            for (float f : x) {
                n += f * f;
            }
            assertThat(Math.sqrt(n)).isBetween(0.99, 1.01); // đã chuẩn hóa (U3)
        });
    }

    @Test
    void groqDescribesImageInVietnamese() throws Exception {
        VisionClient groq = new GroqVisionClient(RestClient.builder(), new ObjectMapper(),
                "https://api.groq.com/openai/v1", System.getenv("GROQ_API_KEY"),
                Optional.ofNullable(System.getenv("GROQ_VISION_MODEL")).orElse("qwen/qwen3.8-27b"));
        Optional<String> caption = groq.describe(List.of(jpeg()));
        System.out.println("Groq mô tả: " + caption.orElse("(rỗng)"));
        assertThat(caption).isPresent();
    }

    /** Ảnh 256px vẽ một "chiếc ví" nâu có khóa kim loại. */
    private static byte[] jpeg() throws Exception {
        BufferedImage img = new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 256, 256);
        g.setColor(new Color(120, 72, 40));
        g.fillRoundRect(40, 70, 176, 116, 20, 20);
        g.setColor(Color.LIGHT_GRAY);
        g.fillOval(190, 115, 20, 20);
        g.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", out);
        return out.toByteArray();
    }
}
