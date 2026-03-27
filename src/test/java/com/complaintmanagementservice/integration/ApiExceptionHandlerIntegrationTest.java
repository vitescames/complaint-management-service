package com.complaintmanagementservice.integration;

import com.complaintmanagementservice.application.exception.InputValidationException;
import com.complaintmanagementservice.application.port.in.CreateComplaintUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.task.scheduling.enabled=false")
@Import(IntegrationTestConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ApiExceptionHandlerIntegrationTest extends IntegrationTestSupport {

    @MockitoBean
    private CreateComplaintUseCase createComplaintUseCase;

    @Test
    void shouldReturnUnprocessableEntityWhenPostRaisesInputValidationException() throws Exception {
        when(createComplaintUseCase.create(any()))
                .thenThrow(new InputValidationException("O CPF do cliente é obrigatório."));

        mockMvc.perform(post("/complaints")
                        .contentType(APPLICATION_JSON)
                        .content(validRestPayloadWithoutDocuments()))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.title").value("Regra de negócio violada"))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.message").value("O CPF do cliente é obrigatório."));
    }

    @Test
    void shouldReturnFriendlyInternalServerErrorWhenPostRaisesUnexpectedException() throws Exception {
        when(createComplaintUseCase.create(any()))
                .thenThrow(new IllegalStateException("falha técnica"));

        mockMvc.perform(post("/complaints")
                        .contentType(APPLICATION_JSON)
                        .content(validRestPayloadWithoutDocuments()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.title").value("Erro interno"))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("Ocorreu um erro interno. Tente novamente mais tarde."));
    }
}
