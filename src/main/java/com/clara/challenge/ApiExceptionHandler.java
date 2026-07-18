package com.clara.challenge;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiErrorResponse> handleValidation(
      MethodArgumentNotValidException ex, HttpServletRequest request) {
    String message =
        ex.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + " " + error.getDefaultMessage())
            .collect(Collectors.joining("; "));
    return errorResponse(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message, request.getRequestURI());
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
      ConstraintViolationException ex, HttpServletRequest request) {
    return errorResponse(
        HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", ex.getMessage(), request.getRequestURI());
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiErrorResponse> handleIllegalArgument(
      IllegalArgumentException ex, HttpServletRequest request) {
    String message = ex.getMessage() == null ? "Invalid request" : ex.getMessage();
    String lower = message.toLowerCase();

    if (lower.contains("already exists")) {
      return errorResponse(HttpStatus.CONFLICT, "DUPLICATE_EVENT_ID", message, request.getRequestURI());
    }
    if (lower.contains("not found")) {
      return errorResponse(HttpStatus.NOT_FOUND, "TRACE_NOT_FOUND", message, request.getRequestURI());
    }
    return errorResponse(HttpStatus.BAD_REQUEST, "BAD_REQUEST", message, request.getRequestURI());
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
    return errorResponse(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "INTERNAL_SERVER_ERROR",
        "Unexpected error",
        request.getRequestURI());
  }

  private ResponseEntity<ApiErrorResponse> errorResponse(
      HttpStatus status, String code, String message, String path) {
    ApiErrorResponse body =
        new ApiErrorResponse(
            Instant.now().toString(), status.value(), status.getReasonPhrase(), code, message, path);
    return ResponseEntity.status(status).body(body);
  }

  public record ApiErrorResponse(
      String timestamp, int status, String error, String code, String message, String path) {}
}
