package com.intema.demo.controller;

import com.intema.demo.service.EventNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.TreeMap;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> forbidden(Exception ex, HttpServletRequest request) {
        return error(HttpStatus.FORBIDDEN, ex.getMessage(), request);
    }

    @ExceptionHandler(EventNotFoundException.class)
    public ResponseEntity<ProblemDetail> notFound(EventNotFoundException ex, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> validation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        ProblemDetail body = problem(HttpStatus.BAD_REQUEST, "Dati non validi", request);
        Map<String, String> fields = new TreeMap<>();
        ex.getBindingResult().getFieldErrors().forEach(field ->
                fields.put(field.getField(), field.getDefaultMessage()));
        body.setProperty("errors", fields);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class})
    public ResponseEntity<ProblemDetail> invalid(Exception ex, HttpServletRequest request) {
        String detail = ex instanceof IllegalArgumentException ? ex.getMessage()
                : "Parametri o JSON non validi";
        return error(HttpStatus.BAD_REQUEST, detail, request);
    }

    private ResponseEntity<ProblemDetail> error(HttpStatus status, String detail, HttpServletRequest request) {
        return ResponseEntity.status(status).body(problem(status, detail, request));
    }

    private ProblemDetail problem(HttpStatus status, String detail, HttpServletRequest request) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(status, detail);
        body.setTitle(status.getReasonPhrase());
        body.setInstance(URI.create(request.getRequestURI()));
        body.setProperty("timestamp", Instant.now().toString());
        return body;
    }
}
