package com.complaintmanagementservice.application.port.out;

import com.complaintmanagementservice.application.query.SearchComplaintsQuery;
import com.complaintmanagementservice.domain.model.Complaint;

import java.time.LocalDate;
import java.util.List;

public interface ComplaintRepositoryPort {

    Complaint save(Complaint complaint);

    List<Complaint> search(SearchComplaintsQuery query);

    List<Complaint> findNonResolvedComplaintsCreatedOn(LocalDate complaintDate);
}
