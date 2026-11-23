package com.refound.matching.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** Định dạng đã gọi thật ngày 23/11/2026 (U8); mọi lỗi của Groq thành "không có mô tả", không ném. */
class GroqVisionClientTest {

    static final String URL = "https://groq.test/openai/v1";
    static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 1};
    static final ObjectMapper JSON = new ObjectMapper();

    MockRestServiceServer server;
    GroqVisionClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new GroqVisionClient(builder.baseUrl(URL).defaultHeader("User-Agent", "refound-server/1.0").build(),
                JSON, "qwen/qwen3.8-27b");
    }

    @Test
    void sendsUpToThreeImagesAndParsesCaption() {
        server.expect(requestTo(URL + "/chat/completions"))
                .andExpect(header("User-Agent", "refound-server/1.0"))
                .andExpect(jsonPath("$.model").value("qwen/qwen3.8-27b"))
                .andExpect(jsonPath("$.reasoning_effort").value("none"))
                .andExpect(jsonPath("$.response_format.type").value("json_object"))
                .andExpect(jsonPath("$.messages[0].content.length()").value(4)) // lời dặn + 3 ảnh
                .andExpect(jsonPath("$.messages[0].content[1].image_url.url").value("data:image/jpeg;base64,/9j/AQ=="))
                .andRespond(withSuccess(completion("{\"mo_ta\": \"  Ví da   nâu có logo \"}"), MediaType.APPLICATION_JSON));
        assertThat(client.describe(List.of(JPEG, JPEG, JPEG, JPEG))).contains("Ví da nâu có logo");
        server.verify();
    }

    @Test
    void errorsAndBadAnswersGiveNoCaption() {
        server.expect(requestTo(URL + "/chat/completions")).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        assertThat(client.describe(List.of(JPEG))).isEmpty();

        server.reset();
        server.expect(requestTo(URL + "/chat/completions"))
                .andRespond(withSuccess(completion("không phải json"), MediaType.APPLICATION_JSON));
        assertThat(client.describe(List.of(JPEG))).isEmpty();

        server.reset();
        server.expect(requestTo(URL + "/chat/completions"))
                .andRespond(withSuccess(completion("{\"mo_ta\": \"\"}"), MediaType.APPLICATION_JSON));
        assertThat(client.describe(List.of(JPEG))).isEmpty();
    }

    @Test
    void disabledWithoutKeyOrModelOrImages() {
        assertThat(new GroqVisionClient(null, JSON, "m").describe(List.of(JPEG))).isEmpty();
        assertThat(new GroqVisionClient(RestClient.create(), JSON, null).describe(List.of(JPEG))).isEmpty();
        assertThat(client.describe(List.of())).isEmpty(); // không gọi mạng (server không có expectation nào)
        server.verify();
    }

    @Test
    void captionIsCutTo40Words() {
        String long50 = String.join(" ", java.util.Collections.nCopies(50, "từ"));
        assertThat(GroqVisionClient.caption(long50)).hasValueSatisfying(s -> assertThat(s.split(" ")).hasSize(40));
        assertThat(GroqVisionClient.caption("  \n ")).isEqualTo(Optional.empty());
    }

    private static String completion(String content) {
        try {
            return JSON.writeValueAsString(
                    java.util.Map.of("choices", List.of(java.util.Map.of("message", java.util.Map.of("content", content)))));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
