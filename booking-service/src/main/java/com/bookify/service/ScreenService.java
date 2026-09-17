package com.bookify.service;

import com.bookify.entity.Screen;
import com.bookify.entity.Seat;
import com.bookify.entity.Venue;
import com.bookify.enums.SeatCategory;
import com.bookify.exception.InvalidRequestException;
import com.bookify.exception.ResourceAlreadyExistsException;
import com.bookify.exception.ResourceNotFoundException;
import com.bookify.repository.ScreenRepository;
import com.bookify.repository.SeatRepository;
import com.bookify.repository.VenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ScreenService {
    private static final int MAX_ROWS = 26;

    private final ScreenRepository screenRepository;
    private final VenueRepository venueRepository;
    private final SeatRepository seatRepository;

    public Screen addScreen(Screen screen, UUID venueId) {
        Venue venue = venueRepository.findById(venueId)
                .orElseThrow(() -> new ResourceNotFoundException("Venue", venueId));
        screen.setVenue(venue);
        return screenRepository.save(screen);
    }

    public Screen getScreen(UUID id) {
        return screenRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Screen", id));
    }

    @Transactional
    public List<Seat> generateSeatsForScreen(UUID screenId, Short seatsPerRow) {
        Screen screen = screenRepository.findById(screenId)
                .orElseThrow(() -> new ResourceNotFoundException("Screen", screenId));

        if (seatRepository.countByScreenId(screenId) > 0) {
            throw new ResourceAlreadyExistsException("Screen " + screenId + " already has seats.");
        }

        int screenCapacity = screen.getCapacity().intValue();
        int numberOfRows = Math.ceilDivExact(screenCapacity, seatsPerRow);

        if (numberOfRows > MAX_ROWS) {
            throw new InvalidRequestException("Screen " + screenId + " can have only a maximum of " + MAX_ROWS + " rows");
        }

        SeatCategory[] seatCategories = SeatCategory.values();

        List<Seat> seats = new ArrayList<>(screenCapacity);

        for (int i = 0; i < screenCapacity; i++) {
            int row = i / seatsPerRow;
            int number = i % seatsPerRow + 1;
            int seatCategoryIndex = row * seatCategories.length / numberOfRows;

            Seat seat = Seat.builder()
                    .rowId(String.valueOf((char) ('A' + row)))
                    .number((short) number)
                    .seatCategory(seatCategories[seatCategoryIndex])
                    .screen(screen)
                    .build();

            seats.add(seat);
        }

        List<Seat> savedSeats = seatRepository.saveAll(seats);
        screen.setCapacity((short) savedSeats.size());

        return savedSeats;
    }
}
