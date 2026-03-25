package com.complaintmanagementservice.adapters.in.rest;

import com.complaintmanagementservice.adapters.in.rest.error.ApiErrorResponse;
import com.complaintmanagementservice.adapters.in.rest.error.ApiFieldError;
import com.complaintmanagementservice.adapters.in.rest.error.ApiValidationErrorResponse;
import com.complaintmanagementservice.application.exception.BusinessRuleViolationException;
import com.complaintmanagementservice.application.exception.ReferenceDataNotFoundException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.lang.reflect.Method;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void shouldBuildValidationErrorResponses() throws Exception {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Payload(), "payload");
        bindingResult.rejectValue("email", "invalid", "Formato de e-mail invalido");
        bindingResult.rejectValue("cpf", "invalid", "O CPF do cliente e invalido");

        BindException bindException = new BindException(bindingResult);
        ApiValidationErrorResponse bindResponse = handler.handleValidationErrors(bindException);

        Method method = SampleController.class.getDeclaredMethod("sample", Payload.class);
        MethodParameter methodParameter = new MethodParameter(method, 0);
        MethodArgumentNotValidException methodArgumentNotValidException = new MethodArgumentNotValidException(methodParameter, bindingResult);
        ApiValidationErrorResponse methodResponse = handler.handleValidationErrors(methodArgumentNotValidException);

        @SuppressWarnings("unchecked")
        ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
        Path path = mock(Path.class);
        when(path.toString()).thenReturn("search.startDate");
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn("Data invalida");
        ApiValidationErrorResponse constraintResponse = handler.handleValidationErrors(
                new ConstraintViolationException(Set.of(violation))
        );

        assertThat(bindResponse.status()).isEqualTo(400);
        assertThat(bindResponse.errors()).containsExactly(
                new ApiFieldError("cpf", "O CPF do cliente e invalido"),
                new ApiFieldError("email", "Formato de e-mail invalido")
        );
        assertThat(methodResponse.errors()).containsExactly(
                new ApiFieldError("cpf", "O CPF do cliente e invalido"),
                new ApiFieldError("email", "Formato de e-mail invalido")
        );
        assertThat(constraintResponse.errors()).containsExactly(
                new ApiFieldError("startDate", "Data invalida")
        );

        @SuppressWarnings("unchecked")
        ConstraintViolation<Object> rootViolation = mock(ConstraintViolation.class);
        Path rootPath = mock(Path.class);
        when(rootPath.toString()).thenReturn("startDate");
        when(rootViolation.getPropertyPath()).thenReturn(rootPath);
        when(rootViolation.getMessage()).thenReturn("Formato invalido");

        ApiValidationErrorResponse rootConstraintResponse = handler.handleValidationErrors(
                new ConstraintViolationException(Set.of(rootViolation))
        );
        ApiValidationErrorResponse fallbackResponse = handler.handleValidationErrors(new RuntimeException("unexpected"));

        assertThat(rootConstraintResponse.errors()).containsExactly(
                new ApiFieldError("startDate", "Formato invalido")
        );
        assertThat(fallbackResponse.errors()).isEmpty();
    }

    @Test
    void shouldBuildBadRequestResponses() {
        MethodArgumentTypeMismatchException mismatchException = mock(MethodArgumentTypeMismatchException.class);
        when(mismatchException.getName()).thenReturn("startDate");

        ApiValidationErrorResponse fieldErrorResponse = handler.handleTypeMismatch(mismatchException);
        ApiValidationErrorResponse missingParameterResponse = handler.handleMissingRequestParameter(
                new MissingServletRequestParameterException("status", "Integer")
        );
        ApiErrorResponse malformedJsonResponse = handler.handleBadRequest();
        ApiErrorResponse requestValidationResponse = handler.handleBadRequest();

        assertThat(fieldErrorResponse.status()).isEqualTo(400);
        assertThat(fieldErrorResponse.errors())
                .containsExactly(new ApiFieldError("startDate", "Formato invalido para o campo informado"));
        assertThat(missingParameterResponse.errors())
                .containsExactly(new ApiFieldError("status", "Parametro obrigatorio nao informado"));
        assertThat(malformedJsonResponse.message())
                .isEqualTo("Nao foi possivel interpretar a requisicao enviada");
        assertThat(requestValidationResponse.message())
                .isEqualTo("Nao foi possivel interpretar a requisicao enviada");
    }

    @Test
    void shouldBuildMappedBusinessResponses() {
        ApiErrorResponse business = handler.handleBusinessViolation(new BusinessRuleViolationException("violacao"));
        ApiErrorResponse notFound = handler.handleReferenceDataNotFound(new ReferenceDataNotFoundException("nao encontrado"));
        ApiErrorResponse unexpected = handler.handleUnexpectedError(new RuntimeException("boom"));

        assertThat(business.status()).isEqualTo(422);
        assertThat(business.message()).isEqualTo("violacao");
        assertThat(notFound.status()).isEqualTo(404);
        assertThat(notFound.message()).isEqualTo("nao encontrado");
        assertThat(unexpected.status()).isEqualTo(500);
        assertThat(unexpected.message()).isEqualTo("Ocorreu um erro interno. Tente novamente mais tarde.");
    }

    @Test
    void shouldDeclareExpectedResponseStatuses() throws Exception {
        assertThat(responseStatusOf("handleValidationErrors", Exception.class)).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(responseStatusOf("handleTypeMismatch", MethodArgumentTypeMismatchException.class)).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(responseStatusOf("handleMissingRequestParameter", MissingServletRequestParameterException.class)).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(responseStatusOf("handleBadRequest")).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(responseStatusOf("handleBusinessViolation", RuntimeException.class)).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(responseStatusOf("handleReferenceDataNotFound", ReferenceDataNotFoundException.class)).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(responseStatusOf("handleUnexpectedError", Exception.class)).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
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

    private static final class SampleController {
        @SuppressWarnings("unused")
        void sample(Payload payload) {
        }
    }
}
