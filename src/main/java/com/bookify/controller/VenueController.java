package com.bookify.controller;

import com.bookify.dto.VenueRequest;
import com.bookify.dto.VenueResponse;
import com.bookify.entity.Venue;
import com.bookify.service.VenueService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/venues")
public class VenueController {

    private final VenueService venueService;

    public VenueController(VenueService venueService) {
        this.venueService = venueService;
    }

    @PostMapping
    public ResponseEntity<String> addVenue(@RequestBody VenueRequest venueRequest) {

        if (venueRequest.getName() == null || venueRequest.getCity() == null) {
            return ResponseEntity.badRequest().build();
        }

        Venue venue = Venue.builder()
                .name(venueRequest.getName())
                .city(venueRequest.getCity())
                .build();

        venueService.addVenue(venue);

        return new ResponseEntity<>("Venue added successfully", HttpStatus.ACCEPTED);
    }

    @GetMapping("/${id}")
    public ResponseEntity<VenueResponse> getVenue(@PathVariable UUID id) {
        if(id == null) {
            return ResponseEntity.badRequest().build();
        }

        Optional<Venue> venue = venueService.getVenue(id);

        if(venue.isPresent()) {
            VenueResponse venueResponse = VenueResponse
                    .builder()
                    .name(venue.get().getName())
                    .city(venue.get().getCity())
                    .createdAt(venue.get().getCreatedAt())
                    .updatedAt(venue.get().getUpdatedAt())
                    .build();

            return ResponseEntity.ok(venueResponse);
        }

        return ResponseEntity.notFound().build();
    }

}
