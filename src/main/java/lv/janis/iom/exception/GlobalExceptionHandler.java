package lv.janis.iom.exception;

import java.util.List;


import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lv.janis.iom.enums.FailureCode;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex,
                                                     HttpServletRequest req) {

        List<ApiError.FieldIssue> issues = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(err -> new ApiError.FieldIssue(err.getField(), err.getDefaultMessage()))
            .toList();

        return ResponseEntity.badRequest().body(ApiError.validationFailed(req.getRequestURI(), issues));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex,
                                                              HttpServletRequest req) {

        List<ApiError.FieldIssue> issues = ex.getConstraintViolations()
            .stream()
            .map(v -> new ApiError.FieldIssue(
                lastPathNode(v.getPropertyPath().toString()),
                v.getMessage()
            ))
            .toList();

        return ResponseEntity.badRequest().body(ApiError.validationFailed(req.getRequestURI(), issues));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex,
                                                          HttpServletRequest req) {
        return ResponseEntity.badRequest()
            .body(ApiError.badRequest(req.getRequestURI(), ex.getMessage()));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiError> handleBusiness(BusinessException ex,
                                                   HttpServletRequest req) {
        FailureCode code = ex.getCode();
        if (code == FailureCode.PRODUCT_NOT_FOUND) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiError.notFound(req.getRequestURI(), ex.getMessage()));
        }
        if (code == FailureCode.INVENTORY_NOT_FOUND) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiError.notFound(req.getRequestURI(), ex.getMessage()));
        }
        if (code == FailureCode.OUT_OF_STOCK) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiError.conflict(req.getRequestURI(), ex.getMessage()));
        }
        if (code == FailureCode.TECHNICAL_ERROR) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiError.internal(req.getRequestURI(), ex.getMessage()));
        }
        return ResponseEntity.badRequest()
            .body(ApiError.badRequest(req.getRequestURI(), ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiError> handleIllegalState(IllegalStateException ex,
                                                       HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ApiError.conflict(req.getRequestURI(), ex.getMessage()));
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(EntityNotFoundException ex,
                                                   HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiError.notFound(req.getRequestURI(), ex.getMessage()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException ex,
                                                        HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ApiError.conflict(req.getRequestURI(), "Data integrity violation"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex,
                                                     HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiError.internal(req.getRequestURI(), "Unexpected server error"));
    }

    private static String lastPathNode(String path) {
        int i = path.lastIndexOf('.');
        return i >= 0 ? path.substring(i + 1) : path;
    }

    public record ApiError(
        String code,
        String message,
        String path,
        List<FieldIssue> issues
    ) {
        public static ApiError validationFailed(String path, List<FieldIssue> issues) {
            return new ApiError("VALIDATION_FAILED", "Validation failed", path, issues);
        }
        public static ApiError badRequest(String path, String message) {
            return new ApiError("BAD_REQUEST", message, path, List.of());
        }
        public static ApiError conflict(String path, String message) {
            return new ApiError("CONFLICT", message, path, List.of());
        }
        public static ApiError notFound(String path, String message) {
            return new ApiError("NOT_FOUND", message, path, List.of());
        }
        public static ApiError internal(String path, String message) {
            return new ApiError("INTERNAL_ERROR", message, path, List.of());
        }

        public record FieldIssue(String field, String message) {}
    }
}
