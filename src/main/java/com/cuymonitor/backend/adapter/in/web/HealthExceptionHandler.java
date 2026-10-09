package com.cuymonitor.backend.adapter.in.web;

import com.cuymonitor.backend.adapter.in.ingestion.InvalidEventException;
import com.cuymonitor.backend.domain.exception.CageNotFoundException;
import com.cuymonitor.backend.domain.exception.MarkColorAlreadyUsedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Map;

@RestControllerAdvice
public class HealthExceptionHandler {

    @ExceptionHandler(InvalidEventException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidEvent(InvalidEventException ex) {
        return ResponseEntity.badRequest().body(ApiError.body(ApiError.BAD_REQUEST, ex.getMessage()));
    }

    @ExceptionHandler(CageNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleCageNotFound(CageNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiError.body(ApiError.NOT_FOUND, ex.getMessage()));
    }

    // e.g. GET /api/v1/alerts?status=CLOSED
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleBadParameter(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.badRequest()
                .body(ApiError.body(ApiError.BAD_REQUEST, "invalid value for parameter " + ex.getName()));
    }

    @ExceptionHandler(MarkColorAlreadyUsedException.class)
    public ResponseEntity<Map<String, Object>> handleColorUsed(MarkColorAlreadyUsedException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiError.body(ApiError.CONFLICT, ex.getMessage()));
    }
}
