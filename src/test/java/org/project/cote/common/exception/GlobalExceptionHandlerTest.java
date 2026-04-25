package org.project.cote.common.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.project.cote.common.dto.ApiResponse;
import org.project.cote.common.dto.ErrorCode;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("ApiException 은 ErrorCode 의 상태 코드로 응답한다")
    void handleApiException_returnsErrorCodeStatus() {
        ApiException ex = new ApiException(ErrorCode.USER_NOT_FOUND);

        ResponseEntity<ApiResponse<Void>> response = handler.handleApiException(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(false, response.getBody().success());
        assertEquals("USER_NOT_FOUND", response.getBody().error().code());
    }

    @Test
    @DisplayName("MethodArgumentTypeMismatchException 은 400 으로 응답한다 (잘못된 enum 등 query/path 변환 실패)")
    void handleTypeMismatch_returnsBadRequest() throws Exception {
        MethodParameter parameter = sampleMethodParameter();
        MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException(
                "INVALID", String.class, "difficulty", parameter, new IllegalArgumentException("not a valid enum")
        );

        ResponseEntity<ApiResponse<Void>> response = handler.handleTypeMismatch(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("INVALID_REQUEST", response.getBody().error().code());
        assertTrue(response.getBody().error().message().toLowerCase().contains("difficulty"));
    }

    @Test
    @DisplayName("MissingServletRequestParameterException 은 400 으로 응답한다")
    void handleMissingParameter_returnsBadRequest() {
        MissingServletRequestParameterException ex = new MissingServletRequestParameterException("name", "String");

        ResponseEntity<ApiResponse<Void>> response = handler.handleMissingParameter(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("INVALID_REQUEST", response.getBody().error().code());
    }

    @Test
    @DisplayName("처리되지 않은 예외는 500 으로 응답한다")
    void handleException_returnsInternalServerError() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleException(new RuntimeException("boom"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("INTERNAL_SERVER_ERROR", response.getBody().error().code());
    }

    private MethodParameter sampleMethodParameter() throws NoSuchMethodException {
        Method method = SampleSignature.class.getDeclaredMethod("targetMethod", String.class);
        return new MethodParameter(method, 0);
    }

    private static class SampleSignature {
        @SuppressWarnings("unused")
        public void targetMethod(String difficulty) {
        }
    }
}
