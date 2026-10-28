package com.deroyque.activation.infrastructure.provider;

import com.deroyque.activation.infrastructure.observability.ActivationMetrics;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

@Component
@ConditionalOnProperty(name = "provider.stub", havingValue = "false", matchIfMissing = true)
public class RestClientProviderGateway implements ProviderGateway {

    private static final Logger log = LoggerFactory.getLogger(RestClientProviderGateway.class);

    private final RestClient restClient;
    private final ActivationMetrics metrics;

    public RestClientProviderGateway(RestClient providerRestClient, ActivationMetrics metrics) {
        this.restClient = providerRestClient;
        this.metrics = metrics;
    }

    RestClientProviderGateway(RestClient restClient) {
        this(restClient, null);
    }

    @Override
    public ProviderResult activate(ActivateProductCommand command) {
        Timer.Sample sample = metrics == null ? null : metrics.startProviderCall();
        try {
            restClient.post()
                    .uri("/provider/v1/activate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Idempotency-Key", command.externalId())
                    .body(Map.of(
                            "activationRequestId", command.activationRequestId().toString(),
                            "externalId", command.externalId(),
                            "customerId", command.customerId(),
                            "productCode", command.productCode()
                    ))
                    .retrieve()
                    .toBodilessEntity();
            record(sample, "success");
            return new ProviderResult.Success();
        } catch (RestClientResponseException ex) {
            int status = ex.getStatusCode().value();
            log.warn("Provedor retornou HTTP {} para externalId={}", status, command.externalId());
            if (status == 429 || status == 503) {
                record(sample, "rate_limited");
                return new ProviderResult.RateLimited(status);
            }
            if (status >= 400 && status < 500) {
                record(sample, "client_error");
                return new ProviderResult.ClientError(status);
            }
            record(sample, "server_error");
            return new ProviderResult.ServerError(status);
        } catch (ResourceAccessException ex) {
            log.warn("Timeout ou falha de rede ao chamar provedor para externalId={}", command.externalId());
            record(sample, "timeout");
            return new ProviderResult.Timeout();
        }
    }

    private void record(Timer.Sample sample, String outcome) {
        if (metrics != null && sample != null) {
            metrics.recordProviderCall(sample, outcome);
        }
    }
}
