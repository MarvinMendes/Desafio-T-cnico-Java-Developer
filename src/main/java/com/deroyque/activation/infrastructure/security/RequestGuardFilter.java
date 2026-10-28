package com.deroyque.activation.infrastructure.security;

import com.deroyque.activation.api.JwtClientIds;
import com.deroyque.activation.api.ProblemSupport;
import com.deroyque.activation.config.SecurityProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class RequestGuardFilter extends OncePerRequestFilter {

    private final SecurityProperties properties;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public RequestGuardFilter(SecurityProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        int contentLength = request.getContentLength();
        if (contentLength > properties.maxBodyBytes()) {
            write(response, ProblemSupport.problem(
                    HttpStatus.PAYLOAD_TOO_LARGE,
                    "payload-too-large",
                    "Corpo muito grande",
                    "O tamanho do corpo excede o limite permitido.",
                    request
            ));
            return;
        }

        String key = rateLimitKey(request);
        Window window = windows.compute(key, (ignored, current) -> {
            long now = Instant.now().getEpochSecond() / 60;
            if (current == null || current.minute != now) {
                return new Window(now, new AtomicInteger(1));
            }
            current.count.incrementAndGet();
            return current;
        });
        if (window.count.get() > properties.rateLimitPerMinute()) {
            write(response, ProblemSupport.problem(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "rate-limit",
                    "Limite de requisições",
                    "Número de chamadas excedido para esta credencial.",
                    request
            ));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String rateLimitKey(HttpServletRequest request) {
        try {
            return JwtClientIds.current();
        } catch (RuntimeException ex) {
            return request.getRemoteAddr();
        }
    }

    private void write(HttpServletResponse response, ProblemDetail problem) throws IOException {
        response.setStatus(problem.getStatus());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), problem);
    }

    private record Window(long minute, AtomicInteger count) {
    }
}
