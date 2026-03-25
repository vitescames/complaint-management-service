package com.complaintmanagementservice.adapters.in.rest;

import com.complaintmanagementservice.adapters.in.rest.error.ApiErrorResponse;
import com.complaintmanagementservice.adapters.in.rest.error.ApiFieldError;
import com.complaintmanagementservice.adapters.in.rest.error.ApiValidationErrorResponse;
import com.complaintmanagementservice.application.exception.BusinessRuleViolationException;
import com.complaintmanagementservice.application.exception.ReferenceDataNotFoundException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void shouldBuildValidationErrorResponses() throws Exception {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Payload(), "payload");
        bindingResult.addError(new FieldError("payload", "email", "Formato inválido"));
        bindingResult.addError(new FieldError("payload", "cpf", "Formato inválido"));

        ApiValidationErrorResponse bindResponse = handler.handleValidationErrors(new BindException(bindingResult));
        ApiValidationErrorResponse methodResponse = handler.handleValidationErrors(
                new MethodArgumentNotValidException(validationMethodParameter(), bindingResult)
        );

        ConstraintViolation<Object> violation = mockConstraintViolation("search.startDate", "Data inválida");
        ApiValidationErrorResponse constraintResponse = handler.handleValidationErrors(
                new ConstraintViolationException(Set.of(violation))
        );
        ConstraintViolation<Object> rootViolation = mockConstraintViolation("startDate", "Formato inválido");
        ApiValidationErrorResponse rootConstraintResponse = handler.handleValidationErrors(
                new ConstraintViolationException(Set.of(rootViolation))
        );
        ApiValidationErrorResponse fallbackResponse = handler.handleValidationErrors(new RuntimeException("unexpected"));

        assertThat(bindResponse.status()).isEqualTo(400);
        assertThat(bindResponse.errors()).containsExactly(
                new ApiFieldError("cpf", "Formato inválido"),
                new ApiFieldError("email", "Formato inválido")
        );
        assertThat(methodResponse.errors()).containsExactly(
                new ApiFieldError("cpf", "Formato inválido"),
                new ApiFieldError("email", "Formato inválido")
        );
        assertThat(constraintResponse.errors()).containsExactly(
                new ApiFieldError("startDate", "Data inválida")
        );
        assertThat(rootConstraintResponse.errors()).containsExactly(
                new ApiFieldError("startDate", "Formato inválido")
        );
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
        assertThat(fieldErrorResponse.errors())
                .containsExactly(new ApiFieldError("startDate", "Data inválida"));
        assertThat(numericFieldErrorResponse.errors())
                .containsExactly(new ApiFieldError("status", "Formato inválido"));
        assertThat(unknownFieldErrorResponse.errors())
                .containsExactly(new ApiFieldError("customerCpf", "Formato inválido"));
        assertThat(missingParameterResponse.errors())
                .containsExactly(new ApiFieldError("status", "Não pode ser nulo ou vazio"));
        assertThat(unreadableDateResponse.errors())
                .containsExactly(new ApiFieldError("complaintCreatedDate", "Data inválida"));
        assertThat(unreadableStringResponse.errors())
                .containsExactly(new ApiFieldError("status", "Formato inválido"));
        assertThat(unreadableUnknownFieldResponse.message())
                .isEqualTo("Não foi possível interpretar a requisição enviada.");
        assertThat(malformedJsonResponse.message())
                .isEqualTo("Não foi possível interpretar a requisição enviada.");
    }

    @Test
    void shouldBuildMappedBusinessResponses() {
        ApiErrorResponse business = handler.handleBusinessViolation(
                new BusinessRuleViolationException("A data da reclamação não pode ser futura.")
        );
        ApiErrorResponse notFound = handler.handleReferenceDataNotFound(
                new ReferenceDataNotFoundException("Categoria não encontrada.")
        );
        ApiErrorResponse unexpected = handler.handleUnexpectedError(new RuntimeException("boom"));

        assertThat(business.status()).isEqualTo(422);
        assertThat(business.message()).isEqualTo("A data da reclamação não pode ser futura.");
        assertThat(notFound.status()).isEqualTo(404);
        assertThat(notFound.message()).isEqualTo("Categoria não encontrada.");
        assertThat(unexpected.status()).isEqualTo(500);
        assertThat(unexpected.message()).isEqualTo("Ocorreu um erro interno. Tente novamente mais tarde.");
    }

    @Test
    void shouldDeclareExpectedResponseStatuses() throws Exception {
        assertThat(responseStatusOf("handleValidationErrors", Exception.class)).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(responseStatusOf("handleTypeMismatch", MethodArgumentTypeMismatchException.class)).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(responseStatusOf("handleMissingRequestParameter", MissingServletRequestParameterException.class)).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(responseStatusOf("handleUnreadableRequest", HttpMessageNotReadableException.class)).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(responseStatusOf("handleBusinessViolation", RuntimeException.class)).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(responseStatusOf("handleReferenceDataNotFound", ReferenceDataNotFoundException.class)).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(responseStatusOf("handleUnexpectedError", Exception.class)).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ConstraintViolation<Object> mockConstraintViolation(String pathText, String message) {
        ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
        Path path = mock(Path.class);
        when(path.toString()).thenReturn(pathText);
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn(message);
        return violation;
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
        Method method = ApiExceptionHandlerTest.class.getDeclaredMethod("validationPayloadMethod", Payload.class);
        return new MethodParameter(method, 0);
    }

    private HttpStatus responseStatusOf(String methodName, Class<?>... parameterTypes) throws Exception {
        ResponseStatus responseStatus = ApiExceptionHandler.class
                .getDeclaredMethod(methodName, parameterTypes)
                .getAnnotation(ResponseStatus.class);
        return responseStatus.value();
    }

    private void validationPayloadMethod(Payload payload) {
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
}
