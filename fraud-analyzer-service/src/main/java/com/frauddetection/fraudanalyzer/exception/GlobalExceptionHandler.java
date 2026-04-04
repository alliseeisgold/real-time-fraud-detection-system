package com.frauddetection.fraudanalyzer.exception;

import com.frauddetection.common.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(FraudAlertNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> handleFraudAlertNotFound(FraudAlertNotFoundException exception) {
        return ApiResponse.failure("FRAUD_ALERT_NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleUnexpectedException(Exception exception) {
        log.error("Unexpected fraud-analyzer-service error", exception);
        return ApiResponse.failure("INTERNAL_ERROR", "Unexpected service error");
    }
}
