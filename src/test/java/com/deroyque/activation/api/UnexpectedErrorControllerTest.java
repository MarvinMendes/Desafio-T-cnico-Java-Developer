package com.deroyque.activation.api;

import com.deroyque.activation.application.CreateActivationCommand;
import com.deroyque.activation.application.CreateActivationRequestService;
import com.deroyque.activation.application.ProviderIntegrationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UnexpectedErrorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateActivationRequestService createActivationRequestService;

    @Test
    void unexpectedErrorReturns500WithoutStackTrace() throws Exception {
        when(createActivationRequestService.create(any(CreateActivationCommand.class)))
                .thenThrow(new RuntimeException("segredo interno do hibernate"));

        mockMvc.perform(post("/api/v1/requests")
                        .with(JwtAuth.fullAccess("system-a"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "externalId": "EXT-ERR",
                                  "customerId": "CUST-1",
                                  "productCode": "INTERNET-500"
                                }
                                """))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.type").value("https://api.exemplo.com/problems/internal-error"))
                .andExpect(jsonPath("$.detail").value("Ocorreu um erro inesperado."))
                .andExpect(content().string(not(containsString("segredo interno"))))
                .andExpect(content().string(not(containsString("RuntimeException"))));
    }

    @Test
    void integrationErrorReturns502() throws Exception {
        when(createActivationRequestService.create(any(CreateActivationCommand.class)))
                .thenThrow(new ProviderIntegrationException("timeout"));

        mockMvc.perform(post("/api/v1/requests")
                        .with(JwtAuth.fullAccess("system-a"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "externalId": "EXT-INT",
                                  "customerId": "CUST-1",
                                  "productCode": "INTERNET-500"
                                }
                                """))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.type").value("https://api.exemplo.com/problems/integration-error"))
                .andExpect(content().string(not(containsString("timeout"))));
    }
}
