package com.outzdir.in.outzdir.Error;

import java.time.Instant;
import org.springframework.http.HttpStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

@Data
public class ApiError {
    private Instant timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
    
    public ApiError() {
        this.timestamp = Instant.now();
    }

    public ApiError(String message, HttpStatus statusCode, String path) {
        this();
        this.message = message;
        this.status = statusCode.value();
        this.error = statusCode.name();
        this.path = path;
    }

    @JsonIgnore
    public HttpStatus getStatusCode() {
        return HttpStatus.valueOf(this.status);
    }
}
