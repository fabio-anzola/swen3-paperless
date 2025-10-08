package at.technikum.swen3;

import at.technikum.swen3.endpoint.exceptionhandler.GlobalExceptionHandler;
import at.technikum.swen3.exception.ControllerException;
import at.technikum.swen3.exception.RepositoryException;
import at.technikum.swen3.exception.ServiceException;
import at.technikum.swen3.exception.UserCreationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private WebRequest webRequest;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setRequestURI("/test/uri");
        webRequest = new ServletWebRequest(servletRequest);
    }

    @Test
    void handleUserCreationException_returnsUnprocessableEntity() {
        UserCreationException ex = new UserCreationException("User creation failed", null);
        ResponseEntity<Object> response = handler.handleUserCreationException(ex, webRequest);

        assertEquals(422, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("User creation failed"));
    }

    @Test
    void handleServiceException_returnsBadRequest() {
        ServiceException ex = new ServiceException("Service error");
        ResponseEntity<Object> response = handler.handleServiceException(ex, webRequest);

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("Service error"));
    }

    @Test
    void handleRepositoryException_returnsInternalServerError() {
        RepositoryException ex = new RepositoryException("Repository error");
        ResponseEntity<Object> response = handler.handleRepositoryException(ex, webRequest);

        assertEquals(500, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("Repository error"));
    }

    @Test
    void handleControllerException_returnsBadRequest() {
        ControllerException ex = new ControllerException("Controller error");
        ResponseEntity<Object> response = handler.handleControllerException(ex, webRequest);

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("Controller error"));
    }

    @Test
    void handleAllExceptions_returnsInternalServerError() {
        Exception ex = new Exception("Unknown error");
        ResponseEntity<Object> response = handler.handleAllExceptions(ex, webRequest);

        assertEquals(500, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("An unexpected error occurred."));
    }
}