package com.complaintmanagementservice.adapters.in.rest;

import com.complaintmanagementservice.TestFixtures;
import com.complaintmanagementservice.adapters.in.rest.mapper.ComplaintResponseMapper;
import com.complaintmanagementservice.adapters.in.rest.mapper.CreateComplaintRestRequestMapper;
import com.complaintmanagementservice.adapters.in.rest.mapper.SearchComplaintsQueryMapper;
import com.complaintmanagementservice.application.port.in.CreateComplaintUseCase;
import com.complaintmanagementservice.application.port.in.SearchComplaintsUseCase;
import com.complaintmanagementservice.infrastructure.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

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

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                        new ComplaintController(
                                createComplaintUseCase,
                                searchComplaintsUseCase,
                                new CreateComplaintRestRequestMapper(),
                                new SearchComplaintsQueryMapper(),
                                new ComplaintResponseMapper()
                        )
                )
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

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

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                        new ComplaintController(
                                createComplaintUseCase,
                                searchComplaintsUseCase,
                                new CreateComplaintRestRequestMapper(),
                                new SearchComplaintsQueryMapper(),
                                new ComplaintResponseMapper()
                        )
                )
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(get("/complaints")
                        .param("customerCpf", "52998224725")
                        .param("categories", "acesso", "cobrança")
                        .param("status", "1")
                        .param("startDate", "2026-03-01")
                        .param("endDate", "2026-03-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].complaintId").value("11111111-1111-1111-1111-111111111111"))
                .andExpect(jsonPath("$[0].customer.email").value("maria.silva@example.com"))
                .andExpect(jsonPath("$[0].categories.length()").value(2));
    }

    @Test
    void shouldReturnBadRequestWhenPayloadIsInvalid() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                        new ComplaintController(
                                mock(CreateComplaintUseCase.class),
                                mock(SearchComplaintsUseCase.class),
                                new CreateComplaintRestRequestMapper(),
                                new SearchComplaintsQueryMapper(),
                                new ComplaintResponseMapper()
                        )
                )
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

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
                .andExpect(status().isBadRequest());
    }
}
