package com.bookify.controller;

import com.bookify.dto.ScreenRequest;
import com.bookify.dto.ScreenResponse;
import com.bookify.entity.Screen;
import com.bookify.entity.Venue;
import com.bookify.service.ScreenService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
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

        ScreenResponse screenResponse = ScreenResponse
                .builder()
                .id(savedScreen.getId())
                .name(savedScreen.getName())
                .type(savedScreen.getType())
                .capacity(savedScreen.getCapacity())
                .venue(savedScreen.getVenue())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(screenResponse);
    }

    @GetMapping("{id}")
    public ResponseEntity<ScreenResponse> getScreen(@PathVariable UUID id) {
        if (id == null) {
            return ResponseEntity.badRequest().build();
        }

        Optional<Screen> screenOptional = screenService.getScreen(id);

        if (screenOptional.isPresent()) {
            Screen screen = screenOptional.get();

            ScreenResponse screenResponse = ScreenResponse
                    .builder()
                    .id(screen.getId())
                    .name(screen.getName())
                    .type(screen.getType())
                    .capacity(screen.getCapacity())
                    .venue(screen.getVenue())
                    .build();

            return ResponseEntity.ok(screenResponse);
        }

        return ResponseEntity.notFound().build();
    }
}
