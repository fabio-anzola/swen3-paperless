package at.technikum.swen3;

import at.technikum.swen3.endpoint.exceptionhandler.GlobalExceptionHandler;
import at.technikum.swen3.endpoint.exceptionhandler.ErrorResponse;
import at.technikum.swen3.exception.UserCreationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler globalExceptionHandler;

    @BeforeEach
    void setUp() {
        globalExceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    void givenUserCreationException_whenHandleException_thenStatus422() {
        UserCreationException exception = new UserCreationException("User creation failed", null);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/test/user-creation");
        ServletWebRequest webRequest = new ServletWebRequest(request);

        ResponseEntity<Object> response = globalExceptionHandler.handleUserCreationException(exception, webRequest);

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
        assertEquals("application/json", Objects.requireNonNull(response.getHeaders().getContentType()).toString());
        ErrorResponse errorResponse = (ErrorResponse) response.getBody();
        assertEquals(422, errorResponse.status());
        assertEquals("UNPROCESSABLE_ENTITY", errorResponse.error());
        assertEquals("User creation failed", errorResponse.message());
        assertEquals("/test/user-creation", errorResponse.path());
    }

    @Test
    void givenUnhandledException_whenHandleException_thenStatus500() {
        RuntimeException exception = new RuntimeException("Some unexpected error");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/test/unhandled");
        ServletWebRequest webRequest = new ServletWebRequest(request);

        ResponseEntity<Object> response = globalExceptionHandler.handleAllExceptions(exception, webRequest);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("application/json", Objects.requireNonNull(response.getHeaders().getContentType()).toString());
        ErrorResponse errorResponse = (ErrorResponse) response.getBody();
        assertEquals(500, errorResponse.status());
        assertEquals("INTERNAL_SERVER_ERROR", errorResponse.error());
        assertEquals("An unexpected error occurred.", errorResponse.message());
        assertEquals("/test/unhandled", errorResponse.path());
    }
}
