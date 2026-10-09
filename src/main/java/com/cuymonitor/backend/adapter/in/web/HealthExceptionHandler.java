package com.cuymonitor.backend.adapter.in.web;

import com.cuymonitor.backend.adapter.in.ingestion.InvalidEventException;
import com.cuymonitor.backend.domain.exception.CageNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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
}
