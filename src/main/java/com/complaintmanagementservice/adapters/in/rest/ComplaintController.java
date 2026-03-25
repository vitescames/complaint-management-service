package com.complaintmanagementservice.adapters.in.rest;

import com.complaintmanagementservice.adapters.in.rest.dto.ComplaintSearchResponse;
import com.complaintmanagementservice.adapters.in.rest.dto.CreateComplaintRestRequest;
import com.complaintmanagementservice.adapters.in.rest.dto.CreateComplaintRestResponse;
import com.complaintmanagementservice.adapters.in.rest.mapper.ComplaintResponseMapper;
import com.complaintmanagementservice.adapters.in.rest.mapper.CreateComplaintRestRequestMapper;
import com.complaintmanagementservice.adapters.in.rest.mapper.SearchComplaintsQueryMapper;
import com.complaintmanagementservice.application.port.in.CreateComplaintUseCase;
import com.complaintmanagementservice.application.port.in.SearchComplaintsUseCase;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/complaints")
public class ComplaintController {

    private final CreateComplaintUseCase createComplaintUseCase;
    private final SearchComplaintsUseCase searchComplaintsUseCase;
    private final CreateComplaintRestRequestMapper createComplaintRestRequestMapper;
    private final SearchComplaintsQueryMapper searchComplaintsQueryMapper;
    private final ComplaintResponseMapper complaintResponseMapper;

    public ComplaintController(
            CreateComplaintUseCase createComplaintUseCase,
            SearchComplaintsUseCase searchComplaintsUseCase,
            CreateComplaintRestRequestMapper createComplaintRestRequestMapper,
            SearchComplaintsQueryMapper searchComplaintsQueryMapper,
            ComplaintResponseMapper complaintResponseMapper
    ) {
        this.createComplaintUseCase = createComplaintUseCase;
        this.searchComplaintsUseCase = searchComplaintsUseCase;
        this.createComplaintRestRequestMapper = createComplaintRestRequestMapper;
        this.searchComplaintsQueryMapper = searchComplaintsQueryMapper;
        this.complaintResponseMapper = complaintResponseMapper;
    }

    @PostMapping
    public ResponseEntity<CreateComplaintRestResponse> create(@Valid @RequestBody CreateComplaintRestRequest request) {
        CreateComplaintRestResponse response = complaintResponseMapper.toCreateResponse(
                createComplaintUseCase.create(createComplaintRestRequestMapper.toCommand(request))
        );
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{complaintId}")
                .buildAndExpand(response.complaintId())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ComplaintSearchResponse>> search(
            @RequestParam(required = false) String customerCpf,
            @RequestParam(required = false) List<String> categories,
            @RequestParam(required = false, name = "status") List<Integer> statusIds,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ResponseEntity.ok(
                complaintResponseMapper.toSearchResponses(
                        searchComplaintsUseCase.search(
                                searchComplaintsQueryMapper.toQuery(customerCpf, categories, statusIds, startDate, endDate)
                        )
                )
        );
    }
}
