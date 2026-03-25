package com.complaintmanagementservice.application.port.in;

import com.complaintmanagementservice.application.query.SearchComplaintsQuery;
import com.complaintmanagementservice.domain.model.Complaint;

import java.util.List;

public interface SearchComplaintsUseCase {

    List<Complaint> search(SearchComplaintsQuery query);
}
