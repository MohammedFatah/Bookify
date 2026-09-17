package com.bookify.service;

import com.bookify.entity.Booking;
import com.bookify.entity.Show;
import com.bookify.entity.User;
import com.bookify.exception.ResourceNotFoundException;
import com.bookify.repository.BookingRepository;
import com.bookify.repository.ShowRepository;
import com.bookify.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final ShowRepository showRepository;

    public Booking addBooking(UUID userId, UUID showId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        Show show = showRepository.findById(showId)
                .orElseThrow(() -> new ResourceNotFoundException("Show", showId));

        Booking booking = Booking.builder()
                .user(user)
                .show(show)
                .build();

        return booking;
    }

    public Booking getBooking(UUID id) {
        return null;
    }

}
