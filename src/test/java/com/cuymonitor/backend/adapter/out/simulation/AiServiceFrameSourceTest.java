package com.cuymonitor.backend.adapter.out.simulation;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AiServiceFrameSourceTest {

    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, 1, 2, 3};

    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final AiServiceFrameSource source =
            new AiServiceFrameSource(builder, "http://ai-service:8000", "the-key");

    @Test
    void readsTheLastFrameOfTheCageWithTheApiKey() {
        server.expect(requestTo("http://ai-service:8000/ai/v1/frames/latest?cageId=farm-station"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-API-Key", "the-key"))
                .andRespond(withSuccess(JPEG, MediaType.IMAGE_JPEG));

        assertThat(source.latest("farm-station")).hasValueSatisfying(bytes -> assertThat(bytes).isEqualTo(JPEG));
        server.verify();
    }

    @Test
    void noFrameYetIsEmpty() {
        server.expect(requestTo("http://ai-service:8000/ai/v1/frames/latest?cageId=sim-0a1b2c3d"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThat(source.latest("sim-0a1b2c3d")).isEmpty();
    }

    @Test
    void anAiServiceErrorIsEmptyToo() {
        server.expect(requestTo("http://ai-service:8000/ai/v1/frames/latest?cageId=farm-station"))
                .andRespond(withServerError());

        assertThat(source.latest("farm-station")).isEmpty();
    }
}
