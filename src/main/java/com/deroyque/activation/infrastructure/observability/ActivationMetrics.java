package com.deroyque.activation.infrastructure.observability;

import com.deroyque.activation.domain.ActivationStatus;
import com.deroyque.activation.infrastructure.persistence.ActivationRequestRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

@Component
public class ActivationMetrics {

    private final MeterRegistry meterRegistry;
    private final Map<ActivationStatus, Counter> statusCounters = new EnumMap<>(ActivationStatus.class);

    public ActivationMetrics(MeterRegistry meterRegistry, ActivationRequestRepository repository) {
        this.meterRegistry = meterRegistry;
        for (ActivationStatus status : ActivationStatus.values()) {
            statusCounters.put(status, Counter.builder("activation.requests")
                    .description("Solicitações observadas por status")
                    .tag("status", status.name())
                    .register(meterRegistry));
            Gauge.builder("activation.requests.current", repository, repo -> repo.countByStatus(status))
                    .description("Quantidade atual de solicitações por status")
                    .tag("status", status.name())
                    .register(meterRegistry);
        }
    }

    public Timer.Sample startProviderCall() {
        return Timer.start(meterRegistry);
    }

    public void recordProviderCall(Timer.Sample sample, String outcome) {
        sample.stop(Timer.builder("activation.provider.calls")
                .description("Latência das chamadas ao provedor")
                .tag("outcome", outcome)
                .register(meterRegistry));
        if (!"success".equals(outcome)) {
            Counter.builder("activation.provider.errors")
                    .description("Erros e timeouts ao chamar o provedor")
                    .tag("outcome", outcome)
                    .register(meterRegistry)
                    .increment();
        }
    }

    public void recordStatus(ActivationStatus status) {
        if (status != null) {
            statusCounters.get(status).increment();
        }
    }
}
