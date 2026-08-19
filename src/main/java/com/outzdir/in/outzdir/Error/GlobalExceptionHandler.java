package com.outzdir.in.outzdir.Error;

import java.nio.file.AccessDeniedException;

import javax.naming.AuthenticationException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import io.jsonwebtoken.JwtException;

@RestControllerAdvice
public class GlobalExceptionHandler {

      
    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ApiError> handaleUserNameNotFoundException(UsernameNotFoundException ex){
        ApiError apiError = new ApiError("Username not found with username " + ex.getMessage(), HttpStatus.NOT_FOUND);
        return new ResponseEntity<>(apiError, apiError.getStatusCode());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthenticationException(AuthenticationException ex){
        ApiError apiError = new ApiError("Authentication Failed "+ ex.getMessage(), HttpStatus.UNAUTHORIZED);
        return new ResponseEntity<>(apiError, apiError.getStatusCode());
    }

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<ApiError> handaleJwtException(JwtException ex){
        ApiError apiError = new ApiError("Jwt generation failed "+ ex.getMessage(), HttpStatus.UNAUTHORIZED);
        return  new ResponseEntity<>(apiError, apiError.getStatusCode());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handaleAccessDeniedExcepetion(AccessDeniedException ex){
        ApiError apiError = new ApiError("Access denied: Insuffcient Permission ", HttpStatus.FORBIDDEN);
        return new ResponseEntity<>(apiError, apiError.getStatusCode());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handaleGenericException(Exception ex){
        ApiError apiError = new ApiError("Internal Server Error: " + ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR );
        return new ResponseEntity<>(apiError, apiError.getStatusCode());
    }
    
}
