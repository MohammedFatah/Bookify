package com.bookify.service;

import com.bookify.entity.Screen;
import com.bookify.entity.Venue;
import com.bookify.exception.VenueNotFoundException;
import com.bookify.repository.ScreenRepository;
import com.bookify.repository.VenueRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;
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
                .orElseThrow(() -> new VenueNotFoundException("Venue: " + venueId + " not found"));
        screen.setVenue(venue);
        return screenRepository.save(screen);
    }

    public Optional<Screen> getScreen(UUID id) {
        return screenRepository.findById(id);
    }

}
