package com.deroyque.activation.api;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

import java.net.URI;

public final class ProblemSupport {

    public static final String PROBLEM_TYPE_BASE = "https://api.exemplo.com/problems/";

    private ProblemSupport() {
    }

    public static ProblemDetail problem(
            HttpStatus status,
            String typeSlug,
            String title,
            String detail,
            HttpServletRequest request
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(URI.create(PROBLEM_TYPE_BASE + typeSlug));
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("traceId", traceId(request));
        return problem;
    }

    public static ResponseEntity<ProblemDetail> entity(ProblemDetail problem) {
        return ResponseEntity.status(problem.getStatus())
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    public static String traceId(HttpServletRequest request) {
        Object value = request.getAttribute(CorrelationIdFilter.TRACE_ID);
        return value == null ? "unknown" : value.toString();
    }
}
