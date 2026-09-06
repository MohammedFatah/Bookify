package com.bookify.service;

import com.bookify.entity.Venue;
import com.bookify.exception.ResourceNotFoundException;
import com.bookify.repository.VenueRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class VenueService {

    private final VenueRepository venueRepository;

    public VenueService(VenueRepository venueRepository) {
        this.venueRepository = venueRepository;
    }

    public Venue addVenue(Venue venue) {
        return venueRepository.save(venue);
    }

    public Venue getVenue(UUID id) {
        return venueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Venue", id));
    }

}
