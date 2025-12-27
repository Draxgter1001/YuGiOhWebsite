package taf.yugioh.scanner.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import taf.yugioh.scanner.dto.ApiResponse;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handle validation errors from @Valid annotations
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ApiResponse<Map<String, String>> response = new ApiResponse<>(
                false,
                "Validation failed",
                errors
        );

        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Handle illegal argument exceptions
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(
            IllegalArgumentException ex) {

        return ResponseEntity.badRequest().body(
                ApiResponse.error(ex.getMessage())
        );
    }

    /**
     * Handle generic runtime exceptions
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntimeException(RuntimeException ex) {
        // Log the error (in production, use proper logging)
        System.err.println("Runtime exception: " + ex.getMessage());
        ex.printStackTrace();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error("An unexpected error occurred")
        );
    }

    /**
     * Handle all other exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        // Log the error (in production, use proper logging)
        System.err.println("Exception: " + ex.getMessage());
        ex.printStackTrace();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error("An unexpected error occurred")
        );
    }
}