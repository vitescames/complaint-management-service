package com.complaintmanagementservice.adapters.in.rest;

import com.complaintmanagementservice.adapters.in.rest.error.ApiErrorResponse;
import com.complaintmanagementservice.adapters.in.rest.error.ApiFieldError;
import com.complaintmanagementservice.adapters.in.rest.error.ApiValidationErrorResponse;
import com.complaintmanagementservice.application.exception.BusinessRuleViolationException;
import com.complaintmanagementservice.application.exception.ReferenceDataNotFoundException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.complaintmanagementservice.domain.exception.DomainValidationException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Comparator;
import java.util.List;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final String INVALID_DATA_TITLE = "Dados inválidos";
    private static final String BUSINESS_RULE_VIOLATION_TITLE = "Regra de negócio violada";
    private static final String RESOURCE_NOT_FOUND_TITLE = "Recurso não encontrado";
    private static final String INTERNAL_ERROR_TITLE = "Erro interno";
    private static final String INVALID_FIELD_FORMAT_MESSAGE = "Formato inválido";
    private static final String INVALID_DATE_MESSAGE = "Data inválida";
    private static final String MISSING_REQUIRED_PARAMETER_MESSAGE = "Não pode ser nulo ou vazio";
    private static final String UNREADABLE_REQUEST_MESSAGE = "Não foi possível interpretar a requisição enviada.";
    private static final String INTERNAL_ERROR_MESSAGE = "Ocorreu um erro interno. Tente novamente mais tarde.";

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            BindException.class,
            ConstraintViolationException.class
    })
    public ApiValidationErrorResponse handleValidationErrors(Exception exception) {
        return new ApiValidationErrorResponse(
                INVALID_DATA_TITLE,
                HttpStatus.BAD_REQUEST.value(),
                resolveValidationErrors(exception)
        );
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ApiValidationErrorResponse handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
        return new ApiValidationErrorResponse(
                INVALID_DATA_TITLE,
                HttpStatus.BAD_REQUEST.value(),
                List.of(new ApiFieldError(exception.getName(), messageForTargetType(exception.getRequiredType())))
        );
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ApiValidationErrorResponse handleMissingRequestParameter(MissingServletRequestParameterException exception) {
        return new ApiValidationErrorResponse(
                INVALID_DATA_TITLE,
                HttpStatus.BAD_REQUEST.value(),
                List.of(new ApiFieldError(exception.getParameterName(), MISSING_REQUIRED_PARAMETER_MESSAGE))
        );
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Object handleUnreadableRequest(HttpMessageNotReadableException exception) {
        if (exception.getCause() instanceof InvalidFormatException invalidFormatException
                && !invalidFormatException.getPath().isEmpty()) {
            String fieldName = invalidFormatException.getPath().get(invalidFormatException.getPath().size() - 1).getFieldName();
            return new ApiValidationErrorResponse(
                    INVALID_DATA_TITLE,
                    HttpStatus.BAD_REQUEST.value(),
                    List.of(new ApiFieldError(fieldName, messageForTargetType(invalidFormatException.getTargetType())))
            );
        }

        return toApiErrorResponse(HttpStatus.BAD_REQUEST, INVALID_DATA_TITLE, UNREADABLE_REQUEST_MESSAGE);
    }

    @ResponseStatus(HttpStatus.UNPROCESSABLE_CONTENT)
    @ExceptionHandler({
            BusinessRuleViolationException.class,
            DomainValidationException.class
    })
    public ApiErrorResponse handleBusinessViolation(RuntimeException exception) {
        return toApiErrorResponse(
                HttpStatus.UNPROCESSABLE_CONTENT,
                BUSINESS_RULE_VIOLATION_TITLE,
                exception.getMessage()
        );
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(ReferenceDataNotFoundException.class)
    public ApiErrorResponse handleReferenceDataNotFound(ReferenceDataNotFoundException exception) {
        return toApiErrorResponse(
                HttpStatus.NOT_FOUND,
                RESOURCE_NOT_FOUND_TITLE,
                exception.getMessage()
        );
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public ApiErrorResponse handleUnexpectedError(Exception ignored) {
        return toApiErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                INTERNAL_ERROR_TITLE,
                INTERNAL_ERROR_MESSAGE
        );
    }

    private List<ApiFieldError> resolveValidationErrors(Exception exception) {
        if (exception instanceof MethodArgumentNotValidException methodArgumentNotValidException) {
            return methodArgumentNotValidException.getBindingResult().getFieldErrors().stream()
                    .sorted(Comparator.comparing(FieldError::getField))
                    .map(this::toApiFieldError)
                    .toList();
        }

        if (exception instanceof BindException bindException) {
            return bindException.getBindingResult().getFieldErrors().stream()
                    .sorted(Comparator.comparing(FieldError::getField))
                    .map(this::toApiFieldError)
                    .toList();
        }

        if (exception instanceof ConstraintViolationException constraintViolationException) {
            return constraintViolationException.getConstraintViolations().stream()
                    .map(violation -> new ApiFieldError(lastPathSegment(violation.getPropertyPath().toString()), violation.getMessage()))
                    .sorted(Comparator.comparing(ApiFieldError::field))
                    .toList();
        }

        return List.of();
    }

    private ApiFieldError toApiFieldError(FieldError fieldError) {
        return new ApiFieldError(fieldError.getField(), fieldError.getDefaultMessage());
    }

    private ApiErrorResponse toApiErrorResponse(HttpStatus status, String title, String message) {
        return new ApiErrorResponse(title, status.value(), message);
    }

    private String lastPathSegment(String propertyPath) {
        int separatorIndex = propertyPath.lastIndexOf('.');
        return separatorIndex >= 0 ? propertyPath.substring(separatorIndex + 1) : propertyPath;
    }

    private String messageForTargetType(Class<?> targetType) {
        if (targetType != null && java.time.temporal.Temporal.class.isAssignableFrom(targetType)) {
            return INVALID_DATE_MESSAGE;
        }
        return INVALID_FIELD_FORMAT_MESSAGE;
    }
}
