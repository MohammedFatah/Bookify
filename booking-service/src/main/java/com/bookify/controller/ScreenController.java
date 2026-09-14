package com.bookify.controller;

import com.bookify.dto.ScreenRequest;
import com.bookify.dto.ScreenResponse;
import com.bookify.dto.SeatGenerationRequest;
import com.bookify.dto.SeatGenerationResponse;
import com.bookify.dto.VenueSummary;
import com.bookify.entity.Screen;
import com.bookify.entity.Seat;
import com.bookify.service.ScreenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/screens")
@RequiredArgsConstructor
public class ScreenController {

    private final ScreenService screenService;

    @PostMapping
    public ResponseEntity<ScreenResponse> addScreen(@Valid @RequestBody ScreenRequest screenRequest) {
        Screen screen = Screen
                .builder()
                .name(screenRequest.getName())
                .capacity(screenRequest.getCapacity())
                .type(screenRequest.getType())
                .build();

        Screen savedScreen = screenService.addScreen(screen, screenRequest.getVenueId());

        VenueSummary venueSummary = VenueSummary
                .builder()
                .id(savedScreen.getVenue().getId())
                .name(savedScreen.getVenue().getName())
                .build();

        ScreenResponse screenResponse = ScreenResponse
                .builder()
                .id(savedScreen.getId())
                .name(savedScreen.getName())
                .type(savedScreen.getType())
                .capacity(savedScreen.getCapacity())
                .venue(venueSummary)
                .build();

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(screenResponse);
    }

    @GetMapping("{id}")
    public ResponseEntity<ScreenResponse> getScreen(@PathVariable UUID id) {
        Screen screen = screenService.getScreen(id);

        VenueSummary venueSummary = VenueSummary
                .builder()
                .id(screen.getVenue().getId())
                .name(screen.getVenue().getName())
                .build();

        ScreenResponse screenResponse = ScreenResponse
                .builder()
                .id(screen.getId())
                .name(screen.getName())
                .type(screen.getType())
                .capacity(screen.getCapacity())
                .venue(venueSummary)
                .createdAt(screen.getCreatedAt())
                .updatedAt(screen.getUpdatedAt())
                .build();

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(screenResponse);

    }

    @PostMapping("/{id}/seats/generate")
    public ResponseEntity<SeatGenerationResponse> generateSeatsForScreen(@PathVariable UUID id, @Valid @RequestBody SeatGenerationRequest seatGenerationRequest) {
        List<Seat> seats = screenService.generateSeatsForScreen(id, seatGenerationRequest.getSeatsPerRow());

        SeatGenerationResponse seatGenerationResponse = SeatGenerationResponse.builder()
                .screenId(id)
                .seatsPerRow(seatGenerationRequest.getSeatsPerRow())
                .numberOfRows((int) seats.stream().map(Seat::getRowId).distinct().count())
                .totalSeatsCreated(seats.size())
                .build();

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(seatGenerationResponse);
    }
}
