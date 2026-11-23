package com.refound.matching.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.refound.matching.infrastructure.EmbeddingClient.EmbeddingException;
import com.refound.matching.infrastructure.EmbeddingClient.Input;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** Định dạng request/response đã gọi thật ngày 23/11/2026 (U3): một request trong lô cho mỗi phần. */
class GeminiEmbeddingClientTest {

    static final String URL = "https://gemini.test/v1beta";
    static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 1};

    MockRestServiceServer server;
    GeminiEmbeddingClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new GeminiEmbeddingClient(builder.baseUrl(URL).defaultHeader("x-goog-api-key", "k").build(),
                "gemini-embedding-2");
    }

    @Test
    void sendsOneRequestPerPartAndParsesVectors() {
        server.expect(requestTo(URL + "/models/gemini-embedding-2:batchEmbedContents"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-goog-api-key", "k"))
                .andExpect(jsonPath("$.requests.length()").value(2))
                .andExpect(jsonPath("$.requests[0].model").value("models/gemini-embedding-2"))
                .andExpect(jsonPath("$.requests[0].output_dimensionality").value(768))
                .andExpect(jsonPath("$.requests[0].content.parts[0].text").value("title: Ví | text: ví nâu"))
                .andExpect(jsonPath("$.requests[1].content.parts[0].inline_data.mime_type").value("image/jpeg"))
                .andExpect(jsonPath("$.requests[1].content.parts[0].inline_data.data")
                        .value(Base64.getEncoder().encodeToString(JPEG)))
                .andRespond(withSuccess("{\"embeddings\":[{\"values\":" + vector(0.5) + "},{\"values\":" + vector(0.25) + "}]}",
                        MediaType.APPLICATION_JSON));

        List<float[]> out = client.embed(List.of(new Input.Text("title: Ví | text: ví nâu"), new Input.Image(JPEG)));
        assertThat(out).hasSize(2);
        assertThat(out.get(0)).hasSize(768).startsWith(0.5f);
        assertThat(out.get(1)[767]).isEqualTo(0.25f);
        server.verify();
    }

    @Test
    void rateLimitIsFlagged() {
        server.expect(requestTo(URL + "/models/gemini-embedding-2:batchEmbedContents"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        assertThatThrownBy(() -> client.embed(List.of(new Input.Text("x"))))
                .isInstanceOfSatisfying(EmbeddingException.class, e -> assertThat(e.rateLimited()).isTrue());
    }

    @Test
    void serverErrorIsNotRateLimit() {
        server.expect(requestTo(URL + "/models/gemini-embedding-2:batchEmbedContents")).andRespond(withServerError());
        assertThatThrownBy(() -> client.embed(List.of(new Input.Text("x"))))
                .isInstanceOfSatisfying(EmbeddingException.class, e -> assertThat(e.rateLimited()).isFalse());
    }

    @Test
    void wrongCountOrDimensionIsAnError() {
        server.expect(requestTo(URL + "/models/gemini-embedding-2:batchEmbedContents"))
                .andRespond(withSuccess("{\"embeddings\":[{\"values\":" + vector(0.1) + "}]}", MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> client.embed(List.of(new Input.Text("a"), new Input.Text("b"))))
                .isInstanceOf(EmbeddingException.class).hasMessageContaining("số vector");

        server.reset();
        server.expect(requestTo(URL + "/models/gemini-embedding-2:batchEmbedContents"))
                .andRespond(withSuccess("{\"embeddings\":[{\"values\":[0.1,0.2]}]}", MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> client.embed(List.of(new Input.Text("a"))))
                .isInstanceOf(EmbeddingException.class).hasMessageContaining("số chiều");
    }

    @Test
    void blankKeyFailsAtStartup() {
        assertThatThrownBy(() -> new GeminiEmbeddingClient(RestClient.builder(), URL, " ", "m"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("GEMINI_API_KEY");
    }

    private static String vector(double fill) {
        return Collections.nCopies(768, String.valueOf(fill)).toString();
    }
}
