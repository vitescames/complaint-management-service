package com.complaintmanagementservice.application.usecase;

import com.complaintmanagementservice.application.port.in.SearchComplaintsUseCase;
import com.complaintmanagementservice.application.port.out.ComplaintRepositoryPort;
import com.complaintmanagementservice.application.query.SearchComplaintsQuery;
import com.complaintmanagementservice.domain.model.Complaint;

import java.util.List;

public class SearchComplaintsUseCaseImpl implements SearchComplaintsUseCase {

    private final ComplaintRepositoryPort complaintRepositoryPort;

    public SearchComplaintsUseCaseImpl(ComplaintRepositoryPort complaintRepositoryPort) {
        this.complaintRepositoryPort = complaintRepositoryPort;
    }

    @Override
    public List<Complaint> search(SearchComplaintsQuery query) {
        return complaintRepositoryPort.search(query);
    }
}
