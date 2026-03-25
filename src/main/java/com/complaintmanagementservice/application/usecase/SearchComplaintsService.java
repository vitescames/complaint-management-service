package com.complaintmanagementservice.application.usecase;

import com.complaintmanagementservice.application.port.in.SearchComplaintsUseCase;
import com.complaintmanagementservice.application.port.out.ComplaintRepositoryPort;
import com.complaintmanagementservice.application.query.SearchComplaintsQuery;
import com.complaintmanagementservice.domain.model.Complaint;

import java.util.List;
import java.util.Objects;

public class SearchComplaintsService implements SearchComplaintsUseCase {

    private final ComplaintRepositoryPort complaintRepositoryPort;

    public SearchComplaintsService(ComplaintRepositoryPort complaintRepositoryPort) {
        this.complaintRepositoryPort = Objects.requireNonNull(complaintRepositoryPort, "complaintRepositoryPort must not be null");
    }

    @Override
    public List<Complaint> search(SearchComplaintsQuery query) {
        return complaintRepositoryPort.search(query);
    }
}
