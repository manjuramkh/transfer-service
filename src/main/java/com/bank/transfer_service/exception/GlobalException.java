package com.bank.transfer_service.exception;

import org.apache.juli.logging.Log;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalException {

    Log logger = org.apache.juli.logging.LogFactory.getLog(GlobalException.class);

    @ExceptionHandler(AccountException.class)
    public ResponseEntity<ErrorInfo> AccountException(Exception e) {
        final ErrorInfo errorInfo = new ErrorInfo();
        errorInfo.setErrorCode(HttpStatus.NOT_FOUND.value());
        errorInfo.setErrorMessage(e.getMessage());
        errorInfo.setTimestamp(LocalDateTime.now());
        logger.error("NoAccountFoundException: {}", e);

        return new ResponseEntity<>(errorInfo, HttpStatus.NOT_FOUND);
    }
}
