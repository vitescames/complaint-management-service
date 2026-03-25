package com.complaintmanagementservice.adapters.in.rest.mapper;

import com.complaintmanagementservice.TestFixtures;
import com.complaintmanagementservice.adapters.in.rest.dto.ComplaintSearchResponse;
import com.complaintmanagementservice.adapters.in.rest.dto.CreateComplaintRestRequest;
import com.complaintmanagementservice.domain.model.ComplaintStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RestMappersTest {

    private final CreateComplaintRestRequestMapper createComplaintRestRequestMapper = new CreateComplaintRestRequestMapper();
    private final ComplaintResponseMapper complaintResponseMapper = new ComplaintResponseMapper();
    private final SearchComplaintsQueryMapper searchComplaintsQueryMapper = new SearchComplaintsQueryMapper();

    @Test
    void shouldMapRestRequestToCommand() {
        CreateComplaintRestRequest request = new CreateComplaintRestRequest(
                new CreateComplaintRestRequest.CustomerPayload(
                        "529.982.247-25",
                        "Maria Silva",
                        LocalDate.of(1990, 6, 15),
                        "maria@example.com"
                ),
                LocalDate.of(2026, 3, 20),
                "Meu app travando",
                List.of("https://example.com/doc-1")
        );

        var command = createComplaintRestRequestMapper.toCommand(request);

        assertThat(command.customerCpf().value()).isEqualTo("52998224725");
        assertThat(command.documentUrls()).hasSize(1);
        assertThat(command.complaintText().value()).isEqualTo("Meu app travando");

        CreateComplaintRestRequest requestWithoutDocuments = new CreateComplaintRestRequest(
                new CreateComplaintRestRequest.CustomerPayload(
                        "52998224725",
                        "Maria Silva",
                        LocalDate.of(1990, 6, 15),
                        "maria@example.com"
                ),
                LocalDate.of(2026, 3, 20),
                "Sem documentos",
                null
        );

        assertThat(createComplaintRestRequestMapper.toCommand(requestWithoutDocuments).documentUrls()).isEmpty();
    }

    @Test
    void shouldMapComplaintToResponses() {
        var createResponse = complaintResponseMapper.toCreateResponse(TestFixtures.complaint());
        ComplaintSearchResponse searchResponse = complaintResponseMapper.toSearchResponse(TestFixtures.complaint());

        assertThat(createResponse.complaintId()).isEqualTo("11111111-1111-1111-1111-111111111111");
        assertThat(createResponse.statusId()).isEqualTo(1);
        assertThat(createResponse.statusName()).isEqualTo("PENDING");
        assertThat(searchResponse.customer().cpf()).isEqualTo("52998224725");
        assertThat(searchResponse.categories()).hasSize(2);
        assertThat(searchResponse.documentUrls()).containsExactly("https://example.com/doc-1");
        assertThat(complaintResponseMapper.toSearchResponses(List.of(TestFixtures.complaint()))).hasSize(1);
    }

    @Test
    void shouldMapSearchParametersToQuery() {
        var query = searchComplaintsQueryMapper.toQuery(
                "52998224725",
                List.of("acesso"),
                List.of(ComplaintStatus.PENDING.id()),
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31)
        );

        assertThat(query.customerCpf()).contains(TestFixtures.customer().cpf());
        assertThat(query.categoryNames()).containsExactly("acesso");
        assertThat(query.statuses()).containsExactly(ComplaintStatus.PENDING);
        assertThat(query.startDate()).contains(LocalDate.of(2026, 3, 1));
        assertThat(query.endDate()).contains(LocalDate.of(2026, 3, 31));

        var emptyQuery = searchComplaintsQueryMapper.toQuery(null, null, null, null, null);
        assertThat(emptyQuery.customerCpf()).isEmpty();
        assertThat(emptyQuery.categoryNames()).isEmpty();
        assertThat(emptyQuery.statuses()).isEmpty();

        var blankCpfQuery = searchComplaintsQueryMapper.toQuery(" ", List.of("acesso"), null, null, LocalDate.of(2026, 3, 31));
        assertThat(blankCpfQuery.customerCpf()).isEmpty();
        assertThat(blankCpfQuery.endDate()).contains(LocalDate.of(2026, 3, 31));
    }
}
