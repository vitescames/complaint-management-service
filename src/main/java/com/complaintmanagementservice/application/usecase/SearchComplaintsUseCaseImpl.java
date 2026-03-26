package com.complaintmanagementservice.application.usecase;

import com.complaintmanagementservice.application.exception.BusinessRuleViolationException;
import com.complaintmanagementservice.application.port.in.SearchComplaintsUseCase;
import com.complaintmanagementservice.application.port.out.ComplaintRepositoryPort;
import com.complaintmanagementservice.application.query.SearchComplaintsQuery;
import com.complaintmanagementservice.domain.model.Complaint;
import com.complaintmanagementservice.domain.model.ComplaintStatus;
import com.complaintmanagementservice.domain.model.Cpf;

import java.util.List;

public class SearchComplaintsUseCaseImpl implements SearchComplaintsUseCase {

    private final ComplaintRepositoryPort complaintRepositoryPort;

    public SearchComplaintsUseCaseImpl(ComplaintRepositoryPort complaintRepositoryPort) {
        this.complaintRepositoryPort = complaintRepositoryPort;
    }

    @Override
    public List<Complaint> search(SearchComplaintsQuery query) {
        validate(query);
        return complaintRepositoryPort.search(query);
    }

    private void validate(SearchComplaintsQuery query) {
        if (query.customerCpf() != null) {
            new Cpf(query.customerCpf());
        }
        query.statusIds().forEach(ComplaintStatus::fromId);

        if (query.startDate() != null && query.endDate() != null && query.startDate().isAfter(query.endDate())) {
            throw new BusinessRuleViolationException("A data inicial deve ser menor ou igual à data final.");
        }
    }
}
