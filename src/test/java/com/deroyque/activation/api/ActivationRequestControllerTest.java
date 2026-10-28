package com.deroyque.activation.api;

import com.deroyque.activation.infrastructure.persistence.ActivationRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ActivationRequestControllerTest {

    private static final String BODY = """
            {
              "externalId": "EXT-12345",
              "customerId": "CUST-10001",
              "productCode": "INTERNET-500"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ActivationRequestRepository repository;

    @BeforeEach
    void cleanDatabase() {
        repository.deleteAll();
    }

    @Test
    void createReturns201WithLocationAndPendingStatus() throws Exception {
        mockMvc.perform(post("/api/v1/requests")
                        .with(JwtAuth.fullAccess("system-a"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern(".*/api/v1/requests/.+")))
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.externalId").value("EXT-12345"))
                .andExpect(jsonPath("$.customerId").value("CUST-10001"))
                .andExpect(jsonPath("$.productCode").value("INTERNET-500"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());
    }

    @Test
    void createRejectsMissingFieldsWithProblemDetails() throws Exception {
        mockMvc.perform(post("/api/v1/requests")
                        .with(JwtAuth.fullAccess("system-a"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.type").value("https://api.exemplo.com/problems/validation-error"))
                .andExpect(jsonPath("$.title").value("Dados inválidos"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.traceId").isNotEmpty())
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void getByIdReturns404WhenMissing() throws Exception {
        mockMvc.perform(get("/api/v1/requests/{id}", UUID.randomUUID())
                        .with(JwtAuth.fullAccess("system-a")))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.type").value("https://api.exemplo.com/problems/not-found"));
    }

    @Test
    void getByIdReturnsCreatedRequest() throws Exception {
        String location = mockMvc.perform(post("/api/v1/requests")
                        .with(JwtAuth.fullAccess("system-a"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getHeader("Location");

        mockMvc.perform(get(location).with(JwtAuth.fullAccess("system-a")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.externalId").value("EXT-12345"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void retryWithSamePayloadReturns200AndExistingRecord() throws Exception {
        mockMvc.perform(post("/api/v1/requests")
                        .with(JwtAuth.fullAccess("system-a"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/requests")
                        .with(JwtAuth.fullAccess("system-a"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.externalId").value("EXT-12345"));
    }

    @Test
    void sameExternalIdWithDifferentPayloadReturns409() throws Exception {
        mockMvc.perform(post("/api/v1/requests")
                        .with(JwtAuth.fullAccess("system-a"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/requests")
                        .with(JwtAuth.fullAccess("system-a"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "externalId": "EXT-12345",
                                  "customerId": "CUST-10001",
                                  "productCode": "INTERNET-1000"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.type").value("https://api.exemplo.com/problems/conflict"));
    }

    @Test
    void listFiltersByCustomerId() throws Exception {
        mockMvc.perform(post("/api/v1/requests")
                        .with(JwtAuth.fullAccess("system-a"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/requests")
                        .with(JwtAuth.fullAccess("system-a"))
                        .param("customerId", "CUST-10001")
                        .param("status", "PENDING")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].externalId").value("EXT-12345"));
    }

    @Test
    void anotherClientCannotReadRequest() throws Exception {
        String location = mockMvc.perform(post("/api/v1/requests")
                        .with(JwtAuth.fullAccess("system-a"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getHeader("Location");

        mockMvc.perform(get(location).with(JwtAuth.fullAccess("system-b")))
                .andExpect(status().isNotFound());
    }

    @Test
    void withoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/requests/{id}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.type").value("https://api.exemplo.com/problems/unauthorized"));
    }

    @Test
    void readScopeCannotCreate() throws Exception {
        mockMvc.perform(post("/api/v1/requests")
                        .with(JwtAuth.readOnly("system-a"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.type").value("https://api.exemplo.com/problems/forbidden"));
    }

    @Test
    void writeScopeCannotRead() throws Exception {
        mockMvc.perform(get("/api/v1/requests")
                        .with(JwtAuth.writeOnly("system-a")))
                .andExpect(status().isForbidden());
    }

    @Test
    void unexpectedErrorDoesNotLeakInternals() throws Exception {
        mockMvc.perform(post("/api/v1/requests")
                        .with(JwtAuth.fullAccess("system-a"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not-json"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(not(org.hamcrest.Matchers.containsString("HttpMessageNotReadableException"))))
                .andExpect(jsonPath("$.detail").value("O corpo da requisição é inválido."));
    }
}
