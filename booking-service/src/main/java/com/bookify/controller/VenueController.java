package com.bookify.controller;

import com.bookify.dto.VenueRequest;
import com.bookify.dto.VenueResponse;
import com.bookify.entity.Venue;
import com.bookify.service.VenueService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/venues")
public class VenueController {

    private final VenueService venueService;

    public VenueController(VenueService venueService) {
        this.venueService = venueService;
    }

    @PostMapping
    public ResponseEntity<VenueResponse> addVenue(@Valid @RequestBody VenueRequest venueRequest) {
        Venue venue = Venue.builder()
                .name(venueRequest.getName())
                .city(venueRequest.getCity())
                .build();

        Venue savedVenue = venueService.addVenue(venue);

        VenueResponse venueResponse = VenueResponse
                .builder()
                .id(savedVenue.getId())
                .name(savedVenue.getName())
                .city(savedVenue.getCity())
                .createdAt(savedVenue.getCreatedAt())
                .updatedAt(savedVenue.getUpdatedAt())
                .build();

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(venueResponse);
    }

    @GetMapping("/{id}")
    public ResponseEntity<VenueResponse> getVenue(@PathVariable UUID id) {
        if (id == null) {
            return ResponseEntity.badRequest().build();
        }

        Venue venue = venueService.getVenue(id);

        VenueResponse venueResponse = VenueResponse
                .builder()
                .name(venue.getName())
                .city(venue.getCity())
                .createdAt(venue.getCreatedAt())
                .updatedAt(venue.getUpdatedAt())
                .build();

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(venueResponse);
    }

}
