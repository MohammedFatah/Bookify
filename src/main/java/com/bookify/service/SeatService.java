package com.bookify.service;

import com.bookify.entity.Screen;
import com.bookify.entity.Seat;
import com.bookify.exception.ResourceAlreadyExistsException;
import com.bookify.exception.ResourceNotFoundException;
import com.bookify.repository.ScreenRepository;
import com.bookify.repository.SeatRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class SeatService {

    private final SeatRepository seatRepository;
    private final ScreenRepository screenRepository;

    public SeatService(SeatRepository seatRepository, ScreenRepository screenRepository) {
        this.seatRepository = seatRepository;
        this.screenRepository = screenRepository;
    }

    public Seat addSeat(Seat seat, UUID screenId) {
        boolean seatAlreadyExists = seatRepository.existsByScreenIdAndRowIdAndNumber(screenId, seat.getRowId(), seat.getNumber());

        if (seatAlreadyExists) {
            throw new ResourceAlreadyExistsException("Seat: " + seat.getRowId() + seat.getNumber() + " already exists on screen: " + screenId);
        }

        Screen screen = screenRepository.findById(screenId)
                .orElseThrow(() -> new ResourceNotFoundException("Screen", screenId));

        seat.setScreen(screen);

        return seatRepository.save(seat);
    }

    public Seat getSeat(UUID id) {
        return seatRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Seat", id));
    }

}
