package com.complaintmanagementservice.application.port.out;

import com.complaintmanagementservice.domain.model.Category;

import java.util.List;

public interface CategoryCatalogPort {

    List<Category> loadAll();
}
