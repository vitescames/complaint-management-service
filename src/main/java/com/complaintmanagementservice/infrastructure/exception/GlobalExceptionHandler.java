package com.complaintmanagementservice.infrastructure.exception;

import com.complaintmanagementservice.application.exception.ApplicationValidationException;
import com.complaintmanagementservice.domain.exception.DomainValidationException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({
            ApplicationValidationException.class,
            DomainValidationException.class,
            ConstraintViolationException.class,
            MethodArgumentNotValidException.class,
            BindException.class,
            HttpMessageNotReadableException.class
    })
    public ProblemDetail handleBadRequest(Exception exception) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problemDetail.setTitle("Invalid request");
        problemDetail.setDetail(exception.getMessage());
        return problemDetail;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpectedError(Exception exception) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        problemDetail.setTitle("Unexpected error");
        problemDetail.setDetail(exception.getMessage());
        return problemDetail;
    }
}
