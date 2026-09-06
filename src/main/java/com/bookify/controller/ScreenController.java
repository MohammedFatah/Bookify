package com.bookify.controller;

import com.bookify.dto.ScreenRequest;
import com.bookify.dto.ScreenResponse;
import com.bookify.dto.VenueSummary;
import com.bookify.entity.Screen;
import com.bookify.service.ScreenService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/screens")
public class ScreenController {

    private final ScreenService screenService;

    public ScreenController(ScreenService screenService) {
        this.screenService = screenService;
    }

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
        if (id == null) {
            return ResponseEntity.badRequest().build();
        }

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
                .build();

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(screenResponse);

    }
}
