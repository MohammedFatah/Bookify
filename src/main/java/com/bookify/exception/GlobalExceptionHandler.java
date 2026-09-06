package com.bookify.exception;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(VenueNotFoundException.class)
    public String handleVenueNotFoundException(VenueNotFoundException ex){
        return ex.getMessage();
    }

}
