package com.complaintmanagementservice.adapters.in.rest;

import com.complaintmanagementservice.adapters.in.rest.error.ApiErrorResponse;
import com.complaintmanagementservice.adapters.in.rest.error.ApiFieldError;
import com.complaintmanagementservice.adapters.in.rest.error.ApiValidationErrorResponse;
import com.complaintmanagementservice.application.exception.BusinessRuleViolationException;
import com.complaintmanagementservice.application.exception.ReferenceDataNotFoundException;
import com.complaintmanagementservice.application.exception.RequestValidationException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
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
        ResponseEntity<ApiValidationErrorResponse> bindResponse = handler.handleValidationErrors(bindException);

        Method method = SampleController.class.getDeclaredMethod("sample", Payload.class);
        MethodParameter methodParameter = new MethodParameter(method, 0);
        MethodArgumentNotValidException methodArgumentNotValidException = new MethodArgumentNotValidException(methodParameter, bindingResult);
        ResponseEntity<ApiValidationErrorResponse> methodResponse = handler.handleValidationErrors(methodArgumentNotValidException);

        @SuppressWarnings("unchecked")
        ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
        Path path = mock(Path.class);
        when(path.toString()).thenReturn("search.startDate");
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn("Data invalida");
        ResponseEntity<ApiValidationErrorResponse> constraintResponse = handler.handleValidationErrors(
                new ConstraintViolationException(Set.of(violation))
        );

        assertThat(bindResponse.getStatusCode().value()).isEqualTo(400);
        assertThat(bindResponse.getBody().errors()).containsExactly(
                new ApiFieldError("cpf", "O CPF do cliente e invalido"),
                new ApiFieldError("email", "Formato de e-mail invalido")
        );
        assertThat(methodResponse.getBody().errors()).containsExactly(
                new ApiFieldError("cpf", "O CPF do cliente e invalido"),
                new ApiFieldError("email", "Formato de e-mail invalido")
        );
        assertThat(constraintResponse.getBody().errors()).containsExactly(
                new ApiFieldError("startDate", "Data invalida")
        );

        @SuppressWarnings("unchecked")
        ConstraintViolation<Object> rootViolation = mock(ConstraintViolation.class);
        Path rootPath = mock(Path.class);
        when(rootPath.toString()).thenReturn("startDate");
        when(rootViolation.getPropertyPath()).thenReturn(rootPath);
        when(rootViolation.getMessage()).thenReturn("Formato invalido");

        ResponseEntity<ApiValidationErrorResponse> rootConstraintResponse = handler.handleValidationErrors(
                new ConstraintViolationException(Set.of(rootViolation))
        );
        ResponseEntity<ApiValidationErrorResponse> fallbackResponse = handler.handleValidationErrors(new RuntimeException("unexpected"));

        assertThat(rootConstraintResponse.getBody().errors()).containsExactly(
                new ApiFieldError("startDate", "Formato invalido")
        );
        assertThat(fallbackResponse.getBody().errors()).isEmpty();
    }

    @Test
    void shouldBuildBadRequestResponses() throws Exception {
        MethodArgumentTypeMismatchException mismatchException = mock(MethodArgumentTypeMismatchException.class);
        when(mismatchException.getName()).thenReturn("startDate");

        ResponseEntity<?> fieldErrorResponse = handler.handleBadRequest(mismatchException);
        ResponseEntity<?> missingParameterResponse = handler.handleBadRequest(
                new MissingServletRequestParameterException("status", "Integer")
        );
        ResponseEntity<?> malformedJsonResponse = handler.handleBadRequest(
                new HttpMessageNotReadableException("bad json", mock(org.springframework.http.HttpInputMessage.class))
        );
        ResponseEntity<?> requestValidationResponse = handler.handleBadRequest(
                new RequestValidationException("invalid")
        );

        assertThat(fieldErrorResponse.getStatusCode().value()).isEqualTo(400);
        assertThat(((ApiValidationErrorResponse) fieldErrorResponse.getBody()).errors())
                .containsExactly(new ApiFieldError("startDate", "Formato invalido para o campo informado"));
        assertThat(((ApiValidationErrorResponse) missingParameterResponse.getBody()).errors())
                .containsExactly(new ApiFieldError("status", "Parametro obrigatorio nao informado"));
        assertThat(((ApiErrorResponse) malformedJsonResponse.getBody()).message())
                .isEqualTo("Nao foi possivel interpretar a requisicao enviada");
        assertThat(((ApiErrorResponse) requestValidationResponse.getBody()).message())
                .isEqualTo("Nao foi possivel interpretar a requisicao enviada");
    }

    @Test
    void shouldBuildMappedBusinessResponses() {
        ResponseEntity<ApiErrorResponse> business = handler.handleBusinessViolation(new BusinessRuleViolationException("violacao"));
        ResponseEntity<ApiErrorResponse> notFound = handler.handleReferenceDataNotFound(new ReferenceDataNotFoundException("nao encontrado"));
        ResponseEntity<ApiErrorResponse> unexpected = handler.handleUnexpectedError(new RuntimeException("boom"));

        assertThat(business.getStatusCode().value()).isEqualTo(422);
        assertThat(business.getBody().message()).isEqualTo("violacao");
        assertThat(notFound.getStatusCode().value()).isEqualTo(404);
        assertThat(notFound.getBody().message()).isEqualTo("nao encontrado");
        assertThat(unexpected.getStatusCode().value()).isEqualTo(500);
        assertThat(unexpected.getBody().message()).isEqualTo("Ocorreu um erro interno. Tente novamente mais tarde.");
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
