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

    public ScreenService(ScreenRepository screenRepository, VenueRepository venueRepository) {
        this.screenRepository = screenRepository;
        this.venueRepository = venueRepository;
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

}
