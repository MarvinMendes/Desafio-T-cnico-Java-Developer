package com.deroyque.activation.infrastructure.provider;

import com.deroyque.activation.api.CorrelationIdFilter;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@ConditionalOnProperty(name = "provider.stub", havingValue = "false", matchIfMissing = true)
public class ProviderClientConfig {

    @Bean
    RestClient providerRestClient(ProviderProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.connectTimeout());
        requestFactory.setReadTimeout(properties.readTimeout());
        return RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory)
                .requestInterceptor(correlationInterceptor())
                .build();
    }

    private ClientHttpRequestInterceptor correlationInterceptor() {
        return (request, body, execution) -> {
            String traceId = MDC.get(CorrelationIdFilter.TRACE_ID);
            if (traceId != null && !traceId.isBlank()) {
                request.getHeaders().set(CorrelationIdFilter.HEADER, traceId);
            }
            return execution.execute(request, body);
        };
    }
}
