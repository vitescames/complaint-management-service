package com.complaintmanagementservice.adapters.in.rest;

import com.complaintmanagementservice.TestFixtures;
import com.complaintmanagementservice.adapters.in.rest.mapper.ComplaintResponseMapper;
import com.complaintmanagementservice.adapters.in.rest.mapper.CreateComplaintRestRequestMapper;
import com.complaintmanagementservice.adapters.in.rest.mapper.SearchComplaintsQueryMapper;
import com.complaintmanagementservice.application.exception.BusinessRuleViolationException;
import com.complaintmanagementservice.application.exception.ReferenceDataNotFoundException;
import com.complaintmanagementservice.application.port.in.CreateComplaintUseCase;
import com.complaintmanagementservice.application.port.in.SearchComplaintsUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ComplaintControllerTest {

    @Test
    void shouldCreateComplaintThroughRestController() throws Exception {
        CreateComplaintUseCase createComplaintUseCase = mock(CreateComplaintUseCase.class);
        SearchComplaintsUseCase searchComplaintsUseCase = mock(SearchComplaintsUseCase.class);
        when(createComplaintUseCase.create(any())).thenReturn(TestFixtures.complaint());

        MockMvc mockMvc = mockMvc(createComplaintUseCase, searchComplaintsUseCase);

        mockMvc.perform(post("/complaints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customer": {
                                    "cpf": "52998224725",
                                    "name": "Maria Silva",
                                    "birthDate": "1990-06-15",
                                    "email": "maria@example.com"
                                  },
                                  "complaintCreatedDate": "2026-03-20",
                                  "complaintText": "Nao consigo acessar o app",
                                  "documentUrls": ["https://example.com/doc-1"]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/complaints/11111111-1111-1111-1111-111111111111"))
                .andExpect(jsonPath("$.complaintId").value("11111111-1111-1111-1111-111111111111"))
                .andExpect(jsonPath("$.statusId").value(1))
                .andExpect(jsonPath("$.statusName").value("PENDING"));
    }

    @Test
    void shouldSearchComplaintsThroughRestController() throws Exception {
        CreateComplaintUseCase createComplaintUseCase = mock(CreateComplaintUseCase.class);
        SearchComplaintsUseCase searchComplaintsUseCase = mock(SearchComplaintsUseCase.class);
        when(searchComplaintsUseCase.search(any())).thenReturn(List.of(TestFixtures.complaint()));

        MockMvc mockMvc = mockMvc(createComplaintUseCase, searchComplaintsUseCase);

        mockMvc.perform(get("/complaints")
                        .param("customerCpf", "52998224725")
                        .param("categories", "acesso", "cobranca")
                        .param("status", "1")
                        .param("startDate", "2026-03-01")
                        .param("endDate", "2026-03-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].complaintId").value("11111111-1111-1111-1111-111111111111"))
                .andExpect(jsonPath("$[0].customer.email").value("maria.silva@example.com"))
                .andExpect(jsonPath("$[0].categories.length()").value(2));
    }

    @Test
    void shouldReturnFieldErrorsWhenPayloadIsInvalid() throws Exception {
        MockMvc mockMvc = mockMvc(mock(CreateComplaintUseCase.class), mock(SearchComplaintsUseCase.class));

        mockMvc.perform(post("/complaints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customer": {
                                    "cpf": "",
                                    "name": "",
                                    "birthDate": null,
                                    "email": "bad-email"
                                  },
                                  "complaintCreatedDate": null,
                                  "complaintText": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Dados inválidos"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.length()").value(7));
    }

    @Test
    void shouldReturnBusinessViolationWhenUseCaseRejectsRequest() throws Exception {
        CreateComplaintUseCase createComplaintUseCase = mock(CreateComplaintUseCase.class);
        when(createComplaintUseCase.create(any()))
                .thenThrow(new BusinessRuleViolationException("A data da reclamação não pode ser futura."));

        MockMvc mockMvc = mockMvc(createComplaintUseCase, mock(SearchComplaintsUseCase.class));

        mockMvc.perform(post("/complaints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customer": {
                                    "cpf": "52998224725",
                                    "name": "Maria Silva",
                                    "birthDate": "1990-06-15",
                                    "email": "maria@example.com"
                                  },
                                  "complaintCreatedDate": "2026-03-20",
                                  "complaintText": "Nao consigo acessar o app"
                                }
                                """))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.title").value("Regra de negócio violada"))
                .andExpect(jsonPath("$.message").value("A data da reclamação não pode ser futura."));
    }

    @Test
    void shouldReturnNotFoundWhenReferenceDataIsMissing() throws Exception {
        CreateComplaintUseCase createComplaintUseCase = mock(CreateComplaintUseCase.class);
        when(createComplaintUseCase.create(any()))
                .thenThrow(new ReferenceDataNotFoundException("O catálogo de categorias de reclamação não está configurado."));

        MockMvc mockMvc = mockMvc(createComplaintUseCase, mock(SearchComplaintsUseCase.class));

        mockMvc.perform(post("/complaints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customer": {
                                    "cpf": "52998224725",
                                    "name": "Maria Silva",
                                    "birthDate": "1990-06-15",
                                    "email": "maria@example.com"
                                  },
                                  "complaintCreatedDate": "2026-03-20",
                                  "complaintText": "Nao consigo acessar o app"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso não encontrado"))
                .andExpect(jsonPath("$.message").value("O catálogo de categorias de reclamação não está configurado."));
    }

    @Test
    void shouldReturnFriendlyInternalError() throws Exception {
        CreateComplaintUseCase createComplaintUseCase = mock(CreateComplaintUseCase.class);
        when(createComplaintUseCase.create(any())).thenThrow(new IllegalStateException("technical error"));

        MockMvc mockMvc = mockMvc(createComplaintUseCase, mock(SearchComplaintsUseCase.class));

        mockMvc.perform(post("/complaints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customer": {
                                    "cpf": "52998224725",
                                    "name": "Maria Silva",
                                    "birthDate": "1990-06-15",
                                    "email": "maria@example.com"
                                  },
                                  "complaintCreatedDate": "2026-03-20",
                                  "complaintText": "Nao consigo acessar o app"
                                }
                                """))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.title").value("Erro interno"))
                .andExpect(jsonPath("$.message").value("Ocorreu um erro interno. Tente novamente mais tarde."));
    }

    private MockMvc mockMvc(CreateComplaintUseCase createComplaintUseCase, SearchComplaintsUseCase searchComplaintsUseCase) {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        return MockMvcBuilders.standaloneSetup(
                        new ComplaintController(
                                createComplaintUseCase,
                                searchComplaintsUseCase,
                                new CreateComplaintRestRequestMapper(),
                                new SearchComplaintsQueryMapper(),
                                new ComplaintResponseMapper()
                        )
                )
                .setControllerAdvice(new ApiExceptionHandler())
                .setValidator(validator)
                .build();
    }
}
