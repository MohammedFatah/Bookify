package com.bookify.controller;

import com.bookify.dto.BookingRequest;
import com.bookify.dto.BookingResponse;
import com.bookify.entity.Booking;
import com.bookify.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
public class BookingController {
    
    private final BookingService bookingService;
    
    public ResponseEntity<BookingResponse> addBooking(@Valid @RequestBody BookingRequest bookingRequest) {

        bookingService.addBooking(bookingRequest.getUserId(), bookingRequest.getShowId());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(null);
    }
    
}
