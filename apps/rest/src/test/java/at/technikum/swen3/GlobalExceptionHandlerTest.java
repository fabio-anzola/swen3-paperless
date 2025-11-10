package at.technikum.swen3;

import at.technikum.swen3.endpoint.exceptionhandler.GlobalExceptionHandler;
import at.technikum.swen3.exception.ControllerException;
import at.technikum.swen3.exception.DocumentNotFoundException;
import at.technikum.swen3.exception.RepositoryException;
import at.technikum.swen3.exception.ServiceException;
import at.technikum.swen3.exception.UserCreationException;
import at.technikum.swen3.exception.UserNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;

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
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
    }

    @Test
    void handleUserNotFoundException_returnsNotFound() {
        UserNotFoundException ex = new UserNotFoundException("User not found");
        ResponseEntity<Object> response = handler.handleUserNotFoundException(ex, webRequest);

        assertEquals(404, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("User not found"));
        assertTrue(response.getBody().toString().contains("/test/uri"));
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
    }

    @Test
    void handleDocumentNotFoundException_returnsNotFound() {
        DocumentNotFoundException ex = new DocumentNotFoundException("Document missing");
        ResponseEntity<Object> response = handler.handleDocumentNotFoundException(ex, webRequest);

        assertEquals(404, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("Document missing"));
        assertTrue(response.getBody().toString().contains("/test/uri"));
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
    }

    @Test
    void handleServiceException_returnsBadRequest() {
        ServiceException ex = new ServiceException("Service error");
        ResponseEntity<Object> response = handler.handleServiceException(ex, webRequest);

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("Service error"));
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
    }

    @Test
    void handleRepositoryException_returnsInternalServerError() {
        RepositoryException ex = new RepositoryException("Repository error");
        ResponseEntity<Object> response = handler.handleRepositoryException(ex, webRequest);

        assertEquals(500, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("Repository error"));
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
    }

    @Test
    void handleControllerException_returnsBadRequest() {
        ControllerException ex = new ControllerException("Controller error");
        ResponseEntity<Object> response = handler.handleControllerException(ex, webRequest);

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("Controller error"));
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
    }

    @Test
    void handleResponseStatusException_handlesUnauthorized_returnsUnauthorizedWithBody() {
        ResponseStatusException ex = new ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Not authorized");
        ResponseEntity<Object> response = handler.handleResponseStatusException(ex, webRequest);

        assertEquals(401, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("Not authorized"));
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
    }

    @Test
    void handleResponseStatusException_otherStatus_returnsStatusAndNullBody() {
        ResponseStatusException ex = new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "bad");
        ResponseEntity<Object> response = handler.handleResponseStatusException(ex, webRequest);

        assertEquals(400, response.getStatusCodeValue());
        Object body = response.getBody();
        assertTrue(body == null || body.toString().contains("bad"));
    }

    @Test
    void handleAllExceptions_returnsInternalServerError() {
        Exception ex = new Exception("Unknown error");
        ResponseEntity<Object> response = handler.handleAllExceptions(ex, webRequest);

        assertEquals(500, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("An unexpected error occurred."));
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
    }
}
