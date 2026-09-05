package com.bookify.service;

import com.bookify.entity.Venue;
import com.bookify.repository.VenueRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class VenueService {

    private final VenueRepository venueRepository;

    public VenueService(VenueRepository venueRepository) {
        this.venueRepository = venueRepository;
    }

    public void addVenue(Venue venue) {
        venueRepository.save(venue);
    }

    public Optional<Venue> getVenue(UUID id) {
        return venueRepository.findById(id);
    }

}
