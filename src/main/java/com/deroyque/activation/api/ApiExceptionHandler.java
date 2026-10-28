package com.deroyque.activation.api;

import com.deroyque.activation.application.ActivationRequestNotFoundException;
import com.deroyque.activation.application.ConflictingActivationException;
import com.deroyque.activation.application.ProviderIntegrationException;
import com.deroyque.activation.domain.InvalidStatusTransitionException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        ProblemDetail problem = ProblemSupport.problem(
                HttpStatus.BAD_REQUEST,
                "validation-error",
                "Dados inválidos",
                "Um ou mais campos não passaram na validação.",
                request
        );
        List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.of("field", error.getField(), "message", String.valueOf(error.getDefaultMessage())))
                .toList();
        problem.setProperty("errors", errors);
        return ProblemSupport.entity(problem);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleUnreadable(HttpServletRequest request) {
        return ProblemSupport.entity(ProblemSupport.problem(
                HttpStatus.BAD_REQUEST,
                "validation-error",
                "Dados inválidos",
                "O corpo da requisição é inválido.",
                request
        ));
    }

    @ExceptionHandler(ActivationRequestNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(
            ActivationRequestNotFoundException ex,
            HttpServletRequest request
    ) {
        return ProblemSupport.entity(ProblemSupport.problem(
                HttpStatus.NOT_FOUND,
                "not-found",
                "Registro inexistente",
                "Solicitação não encontrada.",
                request
        ));
    }

    @ExceptionHandler(ConflictingActivationException.class)
    public ResponseEntity<ProblemDetail> handleConflict(
            ConflictingActivationException ex,
            HttpServletRequest request
    ) {
        return ProblemSupport.entity(ProblemSupport.problem(
                HttpStatus.CONFLICT,
                "conflict",
                "Solicitação duplicada",
                "Já existe uma solicitação com este externalId e conteúdo diferente.",
                request
        ));
    }

    @ExceptionHandler(InvalidStatusTransitionException.class)
    public ResponseEntity<ProblemDetail> handleInvalidTransition(
            InvalidStatusTransitionException ex,
            HttpServletRequest request
    ) {
        return ProblemSupport.entity(ProblemSupport.problem(
                HttpStatus.CONFLICT,
                "conflict",
                "Transição inválida",
                "A transição de status solicitada não é permitida.",
                request
        ));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ProblemDetail> handleAuthentication(HttpServletRequest request) {
        return ProblemSupport.entity(ProblemSupport.problem(
                HttpStatus.UNAUTHORIZED,
                "unauthorized",
                "Não autenticado",
                "É necessário um token Bearer válido.",
                request
        ));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDenied(HttpServletRequest request) {
        return ProblemSupport.entity(ProblemSupport.problem(
                HttpStatus.FORBIDDEN,
                "forbidden",
                "Acesso negado",
                "O token não possui o escopo necessário.",
                request
        ));
    }

    @ExceptionHandler(ProviderIntegrationException.class)
    public ResponseEntity<ProblemDetail> handleIntegration(
            ProviderIntegrationException ex,
            HttpServletRequest request
    ) {
        log.warn("Erro de integração traceId={}", ProblemSupport.traceId(request));
        return ProblemSupport.entity(ProblemSupport.problem(
                HttpStatus.BAD_GATEWAY,
                "integration-error",
                "Erro de integração",
                "Falha ao comunicar com o provedor de ativação.",
                request
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Erro inesperado traceId={}", ProblemSupport.traceId(request), ex);
        return ProblemSupport.entity(ProblemSupport.problem(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "internal-error",
                "Erro inesperado",
                "Ocorreu um erro inesperado.",
                request
        ));
    }
}
