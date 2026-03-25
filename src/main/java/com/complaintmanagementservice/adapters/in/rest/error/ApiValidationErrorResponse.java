package com.complaintmanagementservice.adapters.in.rest.error;

import java.util.List;

public record ApiValidationErrorResponse(String title, int status, List<ApiFieldError> errors) {
}
