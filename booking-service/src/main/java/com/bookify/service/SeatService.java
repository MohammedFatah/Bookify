package com.bookify.service;

import com.bookify.entity.Screen;
import com.bookify.entity.Seat;
import com.bookify.exception.ResourceAlreadyExistsException;
import com.bookify.exception.ResourceNotFoundException;
import com.bookify.repository.ScreenRepository;
import com.bookify.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SeatService {

    private final SeatRepository seatRepository;
    private final ScreenRepository screenRepository;

    @Transactional
    public Seat addSeat(Seat seat, UUID screenId) {
        boolean seatAlreadyExists = seatRepository.existsByScreenIdAndRowIdAndNumber(screenId, seat.getRowId(), seat.getNumber());

        if (seatAlreadyExists) {
            throw new ResourceAlreadyExistsException("Seat: " + seat.getRowId() + seat.getNumber() + " already exists on screen: " + screenId);
        }

        Screen screen = screenRepository.findById(screenId)
                .orElseThrow(() -> new ResourceNotFoundException("Screen", screenId));

        seat.setScreen(screen);

        Seat savedSeat = seatRepository.save(seat);

        long capacity = seatRepository.countByScreenId(screenId);
        screen.setCapacity((short) capacity);

        return savedSeat;
    }

    public Seat getSeat(UUID id) {
        return seatRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Seat", id));
    }

}
