package at.technikum.swen3.endpoint.exceptionhandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.lang.invoke.MethodHandles;

@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @ExceptionHandler(value = {at.technikum.swen3.exception.UserCreationException.class})
    public ResponseEntity<Object> handleUserCreationException(RuntimeException ex, WebRequest request) {
        LOGGER.warn(ex.getMessage());

        ServletWebRequest servletWebRequest = (ServletWebRequest) request;
        String requestPath = servletWebRequest.getRequest().getRequestURI();
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.UNPROCESSABLE_ENTITY.value(),
                HttpStatus.UNPROCESSABLE_ENTITY.name(),
                ex.getMessage(),
                requestPath
        );
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        return handleExceptionInternal(ex, errorResponse, headers, HttpStatus.UNPROCESSABLE_ENTITY, request);
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
