package com.quocnva.easymall.exception;

import com.quocnva.easymall.dtos.response.ApiResponse;
import com.quocnva.easymall.util.Translator;

import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

        @ExceptionHandler(AccessDeniedException.class)
        public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException ex) {
                ErrorCode error = ErrorCode.ACCESS_DENIED;
                return ResponseEntity
                                .status(error.getHttpStatus())
                                .body(ApiResponse.<Void>builder()
                                                .code(error.getCode())
                                                .message(Translator.toLocale(error.getMessageKey()))
                                                .build());
        }

        @ExceptionHandler(AppException.class)
        public ResponseEntity<ApiResponse<Void>> handleAppException(AppException ex) {
                log.error("AppException: ", ex);
                String message = Translator.toLocale(ex.getMessageKey());
                return ResponseEntity
                                .status(ex.getHttpStatus())
                                .body(ApiResponse.<Void>builder()
                                                .code(ex.getCode())
                                                .message(message)
                                                .build());
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException ex) {
                String message = ex.getBindingResult().getFieldErrors().stream()
                                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                                .collect(Collectors.joining(", "));

                return ResponseEntity.badRequest()
                                .body(ApiResponse.<Void>builder()
                                                .code(400)
                                                .message(message)
                                                .build());
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
                ErrorCode error = ErrorCode.UNCATEGORIZED_EXCEPTION;
                log.error("Error: ", ex);
                return ResponseEntity
                                .status(error.getHttpStatus())
                                .body(ApiResponse.<Void>builder()
                                                .code(error.getCode())
                                                .message(Translator.toLocale(error.getMessageKey()))
                                                .build());
        }
}
