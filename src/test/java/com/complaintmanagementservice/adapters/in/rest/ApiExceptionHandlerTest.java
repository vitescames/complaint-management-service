package com.complaintmanagementservice.adapters.in.rest;

import com.complaintmanagementservice.adapters.in.rest.error.ApiErrorResponse;
import com.complaintmanagementservice.adapters.in.rest.error.ApiFieldError;
import com.complaintmanagementservice.adapters.in.rest.error.ApiValidationErrorResponse;
import com.complaintmanagementservice.application.exception.BusinessRuleViolationException;
import com.complaintmanagementservice.application.exception.InputValidationException;
import com.complaintmanagementservice.application.exception.ReferenceDataNotFoundException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ApiExceptionHandlerTest {

    private static final String INVALID_FORMAT_MESSAGE = "Formato inválido";
    private static final String INVALID_DATE_MESSAGE = "Data inválida";
    private static final String REQUIRED_MESSAGE = "Não pode ser nulo ou vazio";
    private static final String UNREADABLE_REQUEST_MESSAGE = "Não foi possível interpretar a requisição enviada.";

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void shouldBuildValidationErrorResponses() throws Exception {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Payload(), "payload");
        bindingResult.addError(new FieldError("payload", "email", INVALID_FORMAT_MESSAGE));
        bindingResult.addError(new FieldError("payload", "cpf", INVALID_FORMAT_MESSAGE));

        ApiValidationErrorResponse bindResponse = handler.handleValidationErrors(new BindException(bindingResult));
        ApiValidationErrorResponse methodResponse = handler.handleValidationErrors(
                new MethodArgumentNotValidException(validationMethodParameter(), bindingResult)
        );
        ApiValidationErrorResponse constraintResponse = handler.handleValidationErrors(
                new ConstraintViolationException(Set.of(nestedStartDateViolation()))
        );
        ApiValidationErrorResponse rootConstraintResponse = handler.handleValidationErrors(
                new ConstraintViolationException(Set.of(rootStartDateViolation()))
        );
        ApiValidationErrorResponse fallbackResponse = handler.handleValidationErrors(new RuntimeException("unexpected"));

        assertThat(bindResponse.status()).isEqualTo(400);
        assertThat(bindResponse.errors()).containsExactly(
                new ApiFieldError("cpf", INVALID_FORMAT_MESSAGE),
                new ApiFieldError("email", INVALID_FORMAT_MESSAGE)
        );
        assertThat(methodResponse.errors()).containsExactly(
                new ApiFieldError("cpf", INVALID_FORMAT_MESSAGE),
                new ApiFieldError("email", INVALID_FORMAT_MESSAGE)
        );
        assertThat(constraintResponse.errors()).containsExactly(new ApiFieldError("startDate", INVALID_DATE_MESSAGE));
        assertThat(rootConstraintResponse.errors()).containsExactly(new ApiFieldError("startDate", INVALID_FORMAT_MESSAGE));
        assertThat(fallbackResponse.errors()).isEmpty();
    }

    @Test
    void shouldBuildBadRequestResponses() {
        MethodArgumentTypeMismatchException mismatchException = mock(MethodArgumentTypeMismatchException.class);
        when(mismatchException.getName()).thenReturn("startDate");
        doReturn(LocalDate.class).when(mismatchException).getRequiredType();

        MethodArgumentTypeMismatchException numericMismatchException = mock(MethodArgumentTypeMismatchException.class);
        when(numericMismatchException.getName()).thenReturn("status");
        doReturn(Integer.class).when(numericMismatchException).getRequiredType();

        MethodArgumentTypeMismatchException unknownMismatchException = mock(MethodArgumentTypeMismatchException.class);
        when(unknownMismatchException.getName()).thenReturn("customerCpf");
        doReturn(null).when(unknownMismatchException).getRequiredType();

        ApiValidationErrorResponse fieldErrorResponse = handler.handleTypeMismatch(mismatchException);
        ApiValidationErrorResponse numericFieldErrorResponse = handler.handleTypeMismatch(numericMismatchException);
        ApiValidationErrorResponse unknownFieldErrorResponse = handler.handleTypeMismatch(unknownMismatchException);
        ApiValidationErrorResponse missingParameterResponse = handler.handleMissingRequestParameter(
                new MissingServletRequestParameterException("status", "Integer")
        );
        ApiValidationErrorResponse unreadableDateResponse = (ApiValidationErrorResponse) handler.handleUnreadableRequest(
                unreadableRequestWithInvalidFormat("complaintCreatedDate", "bad-date", LocalDate.class)
        );
        ApiValidationErrorResponse unreadableStringResponse = (ApiValidationErrorResponse) handler.handleUnreadableRequest(
                unreadableRequestWithInvalidFormat("status", "bad-number", Integer.class)
        );
        ApiErrorResponse unreadableUnknownFieldResponse = (ApiErrorResponse) handler.handleUnreadableRequest(
                unreadableRequestWithInvalidFormat(null, "bad-value", Integer.class)
        );
        ApiErrorResponse malformedJsonResponse = (ApiErrorResponse) handler.handleUnreadableRequest(
                new HttpMessageNotReadableException("bad payload", mock(HttpInputMessage.class))
        );

        assertThat(fieldErrorResponse.status()).isEqualTo(400);
        assertThat(fieldErrorResponse.errors()).containsExactly(new ApiFieldError("startDate", INVALID_DATE_MESSAGE));
        assertThat(numericFieldErrorResponse.errors()).containsExactly(new ApiFieldError("status", INVALID_FORMAT_MESSAGE));
        assertThat(unknownFieldErrorResponse.errors()).containsExactly(new ApiFieldError("customerCpf", INVALID_FORMAT_MESSAGE));
        assertThat(missingParameterResponse.errors()).containsExactly(new ApiFieldError("status", REQUIRED_MESSAGE));
        assertThat(unreadableDateResponse.errors())
                .containsExactly(new ApiFieldError("complaintCreatedDate", INVALID_DATE_MESSAGE));
        assertThat(unreadableStringResponse.errors()).containsExactly(new ApiFieldError("status", INVALID_FORMAT_MESSAGE));
        assertThat(unreadableUnknownFieldResponse.message()).isEqualTo(UNREADABLE_REQUEST_MESSAGE);
        assertThat(malformedJsonResponse.message()).isEqualTo(UNREADABLE_REQUEST_MESSAGE);
    }

    @Test
    void shouldBuildMappedResponses() {
        ApiErrorResponse business = handler.handleBusinessViolation(
                new BusinessRuleViolationException("A data da reclamação não pode ser futura.")
        );
        ApiErrorResponse inputValidation = handler.handleBusinessViolation(
                new InputValidationException("A data inicial deve ser menor ou igual à data final.")
        );
        ApiErrorResponse notFound = handler.handleReferenceDataNotFound(
                new ReferenceDataNotFoundException("Categoria não encontrada.")
        );
        ApiErrorResponse routeNotFound = handler.handleNoResourceFound(
                new NoResourceFoundException(HttpMethod.GET, "/rota-inexistente", null)
        );
        ApiErrorResponse unexpected = handler.handleUnexpectedError(new RuntimeException("boom"));

        assertThat(business.status()).isEqualTo(422);
        assertThat(business.message()).isEqualTo("A data da reclamação não pode ser futura.");
        assertThat(inputValidation.status()).isEqualTo(422);
        assertThat(inputValidation.message()).isEqualTo("A data inicial deve ser menor ou igual à data final.");
        assertThat(notFound.status()).isEqualTo(404);
        assertThat(notFound.message()).isEqualTo("Categoria não encontrada.");
        assertThat(routeNotFound.status()).isEqualTo(404);
        assertThat(routeNotFound.message()).isEqualTo("Recurso não encontrado.");
        assertThat(unexpected.status()).isEqualTo(500);
        assertThat(unexpected.message()).isEqualTo("Ocorreu um erro interno. Tente novamente mais tarde.");
    }

    @Test
    void shouldDeclareExpectedResponseStatuses() throws Exception {
        assertThat(responseStatusOf("handleValidationErrors", Exception.class)).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(responseStatusOf("handleTypeMismatch", MethodArgumentTypeMismatchException.class)).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(responseStatusOf("handleMissingRequestParameter", MissingServletRequestParameterException.class))
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(responseStatusOf("handleUnreadableRequest", HttpMessageNotReadableException.class))
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(responseStatusOf("handleBusinessViolation", RuntimeException.class))
                .isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(responseStatusOf("handleReferenceDataNotFound", ReferenceDataNotFoundException.class))
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(responseStatusOf("handleNoResourceFound", NoResourceFoundException.class))
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(responseStatusOf("handleUnexpectedError", Exception.class))
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ConstraintViolation<?> nestedStartDateViolation() {
        return firstViolation(new NestedValidationPayload(new InvalidDatePayload(null)));
    }

    private ConstraintViolation<?> rootStartDateViolation() {
        return firstViolation(new InvalidFormatPayload(null));
    }

    private <T> ConstraintViolation<T> firstViolation(T payload) {
        try (var validatorFactory = Validation.buildDefaultValidatorFactory()) {
            return validatorFactory.getValidator().validate(payload).iterator().next();
        }
    }

    private HttpMessageNotReadableException unreadableRequestWithInvalidFormat(
            String fieldName,
            String invalidValue,
            Class<?> targetType
    ) {
        InvalidFormatException invalidFormatException =
                InvalidFormatException.from(null, "Invalid format", invalidValue, targetType);
        if (fieldName != null) {
            invalidFormatException.prependPath(new Object(), fieldName);
        }
        return new HttpMessageNotReadableException("bad payload", invalidFormatException, mock(HttpInputMessage.class));
    }

    private MethodParameter validationMethodParameter() throws Exception {
        Method method = Payload.class.getDeclaredMethod("setCpf", String.class);
        return new MethodParameter(method, 0);
    }

    private HttpStatus responseStatusOf(String methodName, Class<?>... parameterTypes) throws Exception {
        ResponseStatus responseStatus = ApiExceptionHandler.class
                .getDeclaredMethod(methodName, parameterTypes)
                .getAnnotation(ResponseStatus.class);
        return responseStatus.value();
    }

    private static final class Payload {
        private String cpf;
        private String email;

        public String getCpf() {
            return cpf;
        }

        public void setCpf(String cpf) {
            this.cpf = cpf;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }

    private record NestedValidationPayload(@Valid InvalidDatePayload search) {
    }

    private record InvalidDatePayload(@NotNull(message = INVALID_DATE_MESSAGE) LocalDate startDate) {
    }

    private record InvalidFormatPayload(@NotNull(message = INVALID_FORMAT_MESSAGE) LocalDate startDate) {
    }
}
