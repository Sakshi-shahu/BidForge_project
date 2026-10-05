package com.bidforge.rfq.config;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ControllerAdvice;

import com.bidforge.rfq.dto.ApiResponse;
import com.bidforge.rfq.exception.AuthenticationException;
import com.bidforge.rfq.exception.InvalidCredentialsException;
import com.bidforge.rfq.exception.InvalidOtpException;
import com.bidforge.rfq.exception.InvalidTokenException;
import com.bidforge.rfq.exception.TokenExpiredException;
import com.bidforge.rfq.exception.UserNotFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiResponse> handleUserNotFound(
            UserNotFoundException exception) {

        log.warn("User not found: {}", exception.getMessage());

        return response(
                HttpStatus.NOT_FOUND,
                exception.getMessage());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiResponse> handleInvalidCredentials(
            InvalidCredentialsException exception) {

        log.warn("Invalid login credentials");

        return response(
                HttpStatus.UNAUTHORIZED,
                exception.getMessage());
    }

    @ExceptionHandler(InvalidOtpException.class)
    public ResponseEntity<ApiResponse> handleInvalidOtp(
            InvalidOtpException exception) {

        log.warn("OTP verification failed: {}", exception.getMessage());

        return response(
                HttpStatus.UNAUTHORIZED,
                exception.getMessage());
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ApiResponse> handleInvalidToken(
            InvalidTokenException exception) {

        log.warn("Invalid token: {}", exception.getMessage());

        return response(
                HttpStatus.UNAUTHORIZED,
                exception.getMessage());
    }

    @ExceptionHandler(TokenExpiredException.class)
    public ResponseEntity<ApiResponse> handleTokenExpired(
            TokenExpiredException exception) {

        log.warn("Token expired: {}", exception.getMessage());

        return response(
                HttpStatus.UNAUTHORIZED,
                exception.getMessage());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse> handleAuthenticationException(
            AuthenticationException exception) {

        log.warn("Authentication error: {}", exception.getMessage());

        return response(
                HttpStatus.UNAUTHORIZED,
                exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse> handleValidation(
            MethodArgumentNotValidException exception) {

        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("Invalid request");

        log.warn("Validation failed: {}", message);

        return response(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse> handleConstraintViolation(
            ConstraintViolationException exception) {

        log.warn("Constraint validation failed: {}", exception.getMessage());

        return response(
                HttpStatus.BAD_REQUEST,
                exception.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse> handleBadRequest(
            IllegalArgumentException exception) {

        log.warn("Bad request: {}", exception.getMessage());

        return response(
                HttpStatus.BAD_REQUEST,
                exception.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse> handleUnexpectedException(
            Exception exception) {

        log.error(
                "Unexpected application error",
                exception);

        return response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Something went wrong. Please try again later.");
    }

    private ResponseEntity<ApiResponse> response(
            HttpStatus status,
            String message) {

        return ResponseEntity
                .status(status)
                .body(new ApiResponse(false, message, null));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse> handleNoResourceFound(
            NoResourceFoundException exception) {

        log.debug(
                "Static resource not found: {}",
                exception.getResourcePath()
        );

        return response(
                HttpStatus.NOT_FOUND,
                "Resource not found"
        );
    }
}
