package com.deroyque.activation.infrastructure.provider;

import com.deroyque.activation.api.CorrelationIdFilter;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class RestClientProviderGatewayTest {

    private MockWebServer server;
    private RestClientProviderGateway gateway;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(1));
        factory.setReadTimeout(Duration.ofMillis(300));
        RestClient restClient = RestClient.builder()
                .baseUrl(server.url("/").toString())
                .requestFactory(factory)
                .requestInterceptor((request, body, execution) -> {
                    String traceId = MDC.get(CorrelationIdFilter.TRACE_ID);
                    if (traceId != null) {
                        request.getHeaders().set(CorrelationIdFilter.HEADER, traceId);
                    }
                    return execution.execute(request, body);
                })
                .build();
        gateway = new RestClientProviderGateway(restClient);
        MDC.put(CorrelationIdFilter.TRACE_ID, "trace-test");
    }

    @AfterEach
    void tearDown() throws IOException {
        MDC.clear();
        server.shutdown();
    }

    @Test
    void successReturnsSuccessAndSendsIdempotencyKey() throws InterruptedException {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));
        ActivateProductCommand command = command();

        ProviderResult result = gateway.activate(command);

        assertThat(result).isInstanceOf(ProviderResult.Success.class);
        RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);
        assertThat(request).isNotNull();
        assertThat(request.getPath()).isEqualTo("/provider/v1/activate");
        assertThat(request.getHeader("Idempotency-Key")).isEqualTo("EXT-1");
        assertThat(request.getHeader("X-Request-Id")).isEqualTo("trace-test");
    }

    @Test
    void http400ReturnsClientError() {
        server.enqueue(new MockResponse().setResponseCode(400));

        assertThat(gateway.activate(command())).isEqualTo(new ProviderResult.ClientError(400));
    }

    @Test
    void http500ReturnsServerError() {
        server.enqueue(new MockResponse().setResponseCode(500));

        assertThat(gateway.activate(command())).isEqualTo(new ProviderResult.ServerError(500));
    }

    @Test
    void http429ReturnsRateLimited() {
        server.enqueue(new MockResponse().setResponseCode(429));

        assertThat(gateway.activate(command())).isEqualTo(new ProviderResult.RateLimited(429));
    }

    @Test
    void timeoutReturnsTimeout() {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{}").setHeadersDelay(1, TimeUnit.SECONDS));

        assertThat(gateway.activate(command())).isInstanceOf(ProviderResult.Timeout.class);
    }

    private ActivateProductCommand command() {
        return new ActivateProductCommand(UUID.randomUUID(), "EXT-1", "CUST-1", "INTERNET-500");
    }
}
