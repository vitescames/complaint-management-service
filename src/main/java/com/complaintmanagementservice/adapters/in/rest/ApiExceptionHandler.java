package com.complaintmanagementservice.adapters.in.rest;

import com.complaintmanagementservice.adapters.in.rest.error.ApiErrorResponse;
import com.complaintmanagementservice.adapters.in.rest.error.ApiFieldError;
import com.complaintmanagementservice.adapters.in.rest.error.ApiValidationErrorResponse;
import com.complaintmanagementservice.application.exception.BusinessRuleViolationException;
import com.complaintmanagementservice.application.exception.ReferenceDataNotFoundException;
import com.complaintmanagementservice.application.exception.RequestValidationException;
import com.complaintmanagementservice.domain.exception.DomainValidationException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            BindException.class,
            ConstraintViolationException.class
    })
    public ResponseEntity<ApiValidationErrorResponse> handleValidationErrors(Exception exception) {
        ApiValidationErrorResponse response = new ApiValidationErrorResponse(
                "Dados invalidos",
                HttpStatus.BAD_REQUEST.value(),
                resolveValidationErrors(exception)
        );
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class,
            RequestValidationException.class
    })
    public ResponseEntity<?> handleBadRequest(Exception exception) {
        if (exception instanceof MethodArgumentTypeMismatchException mismatchException) {
            ApiValidationErrorResponse response = new ApiValidationErrorResponse(
                    "Dados invalidos",
                    HttpStatus.BAD_REQUEST.value(),
                    List.of(new ApiFieldError(mismatchException.getName(), "Formato invalido para o campo informado"))
            );
            return ResponseEntity.badRequest().body(response);
        }

        if (exception instanceof MissingServletRequestParameterException missingParameterException) {
            ApiValidationErrorResponse response = new ApiValidationErrorResponse(
                    "Dados invalidos",
                    HttpStatus.BAD_REQUEST.value(),
                    List.of(new ApiFieldError(missingParameterException.getParameterName(), "Parametro obrigatorio nao informado"))
            );
            return ResponseEntity.badRequest().body(response);
        }

        ApiErrorResponse response = new ApiErrorResponse(
                "Dados invalidos",
                HttpStatus.BAD_REQUEST.value(),
                "Nao foi possivel interpretar a requisicao enviada"
        );
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler({
            BusinessRuleViolationException.class,
            DomainValidationException.class
    })
    public ResponseEntity<ApiErrorResponse> handleBusinessViolation(RuntimeException exception) {
        ApiErrorResponse response = new ApiErrorResponse(
                "Regra de negocio violada",
                HttpStatus.UNPROCESSABLE_ENTITY.value(),
                exception.getMessage()
        );
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
    }

    @ExceptionHandler(ReferenceDataNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleReferenceDataNotFound(ReferenceDataNotFoundException exception) {
        ApiErrorResponse response = new ApiErrorResponse(
                "Recurso nao encontrado",
                HttpStatus.NOT_FOUND.value(),
                exception.getMessage()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpectedError(Exception ignored) {
        ApiErrorResponse response = new ApiErrorResponse(
                "Erro interno",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Ocorreu um erro interno. Tente novamente mais tarde."
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
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

        return new ArrayList<>();
    }

    private ApiFieldError toApiFieldError(FieldError fieldError) {
        return new ApiFieldError(fieldError.getField(), fieldError.getDefaultMessage());
    }

    private String lastPathSegment(String propertyPath) {
        int separatorIndex = propertyPath.lastIndexOf('.');
        return separatorIndex >= 0 ? propertyPath.substring(separatorIndex + 1) : propertyPath;
    }
}
