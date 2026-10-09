package com.cuymonitor.backend.adapter.in.web;

import com.cuymonitor.backend.domain.exception.AlertNotFoundException;
import com.cuymonitor.backend.domain.exception.CageNotFoundException;
import com.cuymonitor.backend.domain.exception.GuineaPigNotFoundException;
import com.cuymonitor.backend.domain.exception.ColorAlreadyUsedException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Map;

@RestControllerAdvice
public class DashboardExceptionHandler {

    @ExceptionHandler(CageNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(CageNotFoundException ex) {
        return error(HttpStatus.NOT_FOUND, ApiError.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(GuineaPigNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleGuineaPigNotFound(GuineaPigNotFoundException ex) {
        return error(HttpStatus.NOT_FOUND, ApiError.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(AlertNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleAlertNotFound(AlertNotFoundException ex) {
        return error(HttpStatus.NOT_FOUND, ApiError.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidRequest(InvalidRequestException ex) {
        return error(HttpStatus.BAD_REQUEST, ApiError.BAD_REQUEST, ex.getMessage());
    }

    // e.g. ?status=DONE or /alerts/abc
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleBadParameter(MethodArgumentTypeMismatchException ex) {
        return error(HttpStatus.BAD_REQUEST, ApiError.BAD_REQUEST, "invalid value for " + ex.getName());
    }

    @ExceptionHandler(ColorAlreadyUsedException.class)
    public ResponseEntity<Map<String, Object>> handleColorTaken(ColorAlreadyUsedException ex) {
        return error(HttpStatus.CONFLICT, ApiError.CONFLICT, ex.getMessage());
    }

    // two requests took the same color at once: the unique (cage, color) constraint is the last line of defense
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleIntegrity(DataIntegrityViolationException ex) {
        return error(HttpStatus.CONFLICT, ApiError.CONFLICT, "the request conflicts with existing data");
    }

    private static ResponseEntity<Map<String, Object>> error(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(ApiError.body(code, message));
    }
}
