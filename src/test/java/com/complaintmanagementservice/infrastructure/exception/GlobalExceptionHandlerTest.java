package com.complaintmanagementservice.infrastructure.exception;

import com.complaintmanagementservice.application.exception.ApplicationValidationException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    @Test
    void shouldBuildBadRequestAndInternalErrorResponses() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        var badRequest = handler.handleBadRequest(new ApplicationValidationException("bad request"));
        var unexpected = handler.handleUnexpectedError(new RuntimeException("unexpected"));

        assertThat(badRequest.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(badRequest.getTitle()).isEqualTo("Invalid request");
        assertThat(badRequest.getDetail()).isEqualTo("bad request");
        assertThat(unexpected.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(unexpected.getTitle()).isEqualTo("Unexpected error");
        assertThat(unexpected.getDetail()).isEqualTo("unexpected");
    }
}
