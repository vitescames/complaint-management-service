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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
                .thenThrow(new InputValidationException("O CPF do cliente \u00E9 obrigat\u00F3rio."));

        mockMvc.perform(post("/complaints")
                        .contentType(APPLICATION_JSON)
                        .content(validRestPayloadWithoutDocuments()))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.title").value("Regra de neg\u00F3cio violada"))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.message").value("O CPF do cliente \u00E9 obrigat\u00F3rio."));
    }

    @Test
    void shouldReturnFriendlyInternalServerErrorWhenPostRaisesUnexpectedException() throws Exception {
        when(createComplaintUseCase.create(any()))
                .thenThrow(new IllegalStateException("falha t\u00E9cnica"));

        mockMvc.perform(post("/complaints")
                        .contentType(APPLICATION_JSON)
                        .content(validRestPayloadWithoutDocuments()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.title").value("Erro interno"))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("Ocorreu um erro interno. Tente novamente mais tarde."));
    }

    @Test
    void shouldReturnNotFoundWhenRouteDoesNotExist() throws Exception {
        mockMvc.perform(get("/rota-inexistente"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso n\u00E3o encontrado"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Recurso n\u00E3o encontrado."));
    }
}
