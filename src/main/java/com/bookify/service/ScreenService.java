package com.bookify.service;

import com.bookify.entity.Screen;
import com.bookify.entity.Venue;
import com.bookify.exception.ResourceNotFoundException;
import com.bookify.repository.ScreenRepository;
import com.bookify.repository.VenueRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ScreenService {

    private final ScreenRepository screenRepository;
    private final VenueRepository venueRepository;
    private final SeatService seatService;

    public ScreenService(ScreenRepository screenRepository, VenueRepository venueRepository, SeatService seatService) {
        this.screenRepository = screenRepository;
        this.venueRepository = venueRepository;
        this.seatService = seatService;
    }

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

    public void generateSeatsForScreen(UUID screenId, int seatsPerRow) {
        Screen screen = screenRepository.findById(screenId)
                .orElseThrow(() -> new ResourceNotFoundException("Screen", screenId));

        int numberOfRows = (int) (double) (screen.getCapacity() / seatsPerRow);

        for (int i=0; i < screen.getCapacity(); i++) {
            char rowId = (char)('A' + i);
            int number = i+1;
        }

//        seatService.addSeat(seat, screenId);
    }
}
