package com.warrantywave.controller;

import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException exception) {
        String detail = exception.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .distinct()
            .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body(Map.of("detail", detail));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleUnreadableRequest() {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(Map.of("detail", "Request body is invalid. Check the field values and try again."));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleResponseStatus(ResponseStatusException exception) {
        String detail = exception.getReason() != null ? exception.getReason() : exception.getStatusCode().toString();
        return ResponseEntity.status(exception.getStatusCode()).body(Map.of("detail", detail));
    }
}