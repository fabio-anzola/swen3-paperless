package at.technikum.swen3.endpoint.exceptionhandler;

import at.technikum.swen3.exception.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.lang.invoke.MethodHandles;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @ExceptionHandler(value = {UserCreationException.class})
    public ResponseEntity<Object> handleUserCreationException(RuntimeException ex, WebRequest request) {
        LOGGER.warn(ex.getMessage());

        ServletWebRequest servletWebRequest = (ServletWebRequest) request;
        String requestPath = servletWebRequest.getRequest().getRequestURI();
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.name(),
                ex.getMessage(),
                requestPath
        );
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        return handleExceptionInternal(ex, errorResponse, headers, HttpStatus.UNPROCESSABLE_ENTITY, request);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Object> handleUserNotFoundException(UserNotFoundException ex, WebRequest request) {
        LOGGER.warn("User not found: {}", ex.getMessage());

        ServletWebRequest servletWebRequest = (ServletWebRequest) request;
        String requestPath = servletWebRequest.getRequest().getRequestURI();
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.name(),
                ex.getMessage(),
                requestPath
        );
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        return handleExceptionInternal(ex, errorResponse, headers, HttpStatus.NOT_FOUND, request);
    }

    @ExceptionHandler(DocumentNotFoundException.class)
    public ResponseEntity<Object> handleDocumentNotFoundException(DocumentNotFoundException ex, WebRequest request) {
        LOGGER.warn("Document not found: {}", ex.getMessage());

        ServletWebRequest servletWebRequest = (ServletWebRequest) request;
        String requestPath = servletWebRequest.getRequest().getRequestURI();
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.name(),
                ex.getMessage(),
                requestPath
        );
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        return handleExceptionInternal(ex, errorResponse, headers, HttpStatus.NOT_FOUND, request);
    }


    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<Object> handleServiceException(ServiceException ex, WebRequest request) {
        LOGGER.error("Service exception: ", ex);

        ServletWebRequest servletWebRequest = (ServletWebRequest) request;
        String requestPath = servletWebRequest.getRequest().getRequestURI();
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.name(),
                ex.getMessage(),
                requestPath
        );
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        return handleExceptionInternal(ex, errorResponse, headers, HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(RepositoryException.class)
    public ResponseEntity<Object> handleRepositoryException(RepositoryException ex, WebRequest request) {
        LOGGER.error("Repository exception: ", ex);

        ServletWebRequest servletWebRequest = (ServletWebRequest) request;
        String requestPath = servletWebRequest.getRequest().getRequestURI();
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.name(),
                ex.getMessage(),
                requestPath
        );
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        return handleExceptionInternal(ex, errorResponse, headers, HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    @ExceptionHandler(ControllerException.class)
    public ResponseEntity<Object> handleControllerException(ControllerException ex, WebRequest request) {
        LOGGER.error("Controller exception: ", ex);

        ServletWebRequest servletWebRequest = (ServletWebRequest) request;
        String requestPath = servletWebRequest.getRequest().getRequestURI();
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.name(),
                ex.getMessage(),
                requestPath
        );
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        return handleExceptionInternal(ex, errorResponse, headers, HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public ResponseEntity<Object> handleResponseStatusException(org.springframework.web.server.ResponseStatusException ex, WebRequest request) {
        if (ex.getStatusCode() == HttpStatus.UNAUTHORIZED) {
            LOGGER.warn("Unauthorized: {}", ex.getReason());
            ServletWebRequest servletWebRequest = (ServletWebRequest) request;
            String requestPath = servletWebRequest.getRequest().getRequestURI();
            ErrorResponse errorResponse = new ErrorResponse(
                    HttpStatus.UNAUTHORIZED.value(),
                    HttpStatus.UNAUTHORIZED.name(),
                    ex.getReason(),
                    requestPath
            );
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            return handleExceptionInternal(ex, errorResponse, headers, HttpStatus.UNAUTHORIZED, request);
        }
        return super.handleExceptionInternal(ex, null, new HttpHeaders(), ex.getStatusCode(), request);
    }


    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleAllExceptions(Exception ex, WebRequest request) {
        LOGGER.error("Unhandled exception: ", ex);

        ServletWebRequest servletWebRequest = (ServletWebRequest) request;
        String requestPath = servletWebRequest.getRequest().getRequestURI();
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.name(),
                "An unexpected error occurred.",
                requestPath
        );
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        return handleExceptionInternal(ex, errorResponse, headers, HttpStatus.INTERNAL_SERVER_ERROR, request);
    }
}
