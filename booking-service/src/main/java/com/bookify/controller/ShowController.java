package com.bookify.controller;

import com.bookify.dto.MovieSummary;
import com.bookify.dto.ScreenSummary;
import com.bookify.dto.ShowRequest;
import com.bookify.dto.ShowResponse;
import com.bookify.entity.Show;
import com.bookify.service.ShowService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/shows")
public class ShowController {

    private final ShowService showService;

    public ShowController(ShowService showService) {
        this.showService = showService;
    }

    @PostMapping
    public ResponseEntity<ShowResponse> addShow(@Valid @RequestBody ShowRequest showRequest) {
        Show show = Show.builder()
                .showTime(showRequest.getShowTime())
                .build();

        Show savedShow = showService.addShow(show, showRequest.getScreenId(), showRequest.getMovieId());

        ScreenSummary screenSummary = ScreenSummary
                .builder()
                .id(savedShow.getScreen().getId())
                .name(savedShow.getScreen().getName())
                .type(savedShow.getScreen().getType())
                .build();

        MovieSummary movieSummary = MovieSummary
                .builder()
                .id(savedShow.getMovie().getId())
                .title(savedShow.getMovie().getTitle())
                .durationMinutes(savedShow.getMovie().getDurationMinutes())
                .build();

        ShowResponse showResponse = ShowResponse
                .builder()
                .id(savedShow.getId())
                .showTime(savedShow.getShowTime())
                .screenSummary(screenSummary)
                .movieSummary(movieSummary)
                .createdAt(savedShow.getCreatedAt())
                .updatedAt(savedShow.getUpdatedAt())
                .build();

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(showResponse);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ShowResponse> getShow(@PathVariable UUID id) {
        if (id == null) {
            return ResponseEntity.badRequest().build();
        }

        Show show = showService.getShow(id);

        ScreenSummary screenSummary = ScreenSummary
                .builder()
                .id(show.getScreen().getId())
                .name(show.getScreen().getName())
                .type(show.getScreen().getType())
                .build();

        MovieSummary movieSummary = MovieSummary
                .builder()
                .id(show.getMovie().getId())
                .title(show.getMovie().getTitle())
                .durationMinutes(show.getMovie().getDurationMinutes())
                .build();

        ShowResponse showResponse = ShowResponse
                .builder()
                .id(show.getId())
                .showTime(show.getShowTime())
                .screenSummary(screenSummary)
                .movieSummary(movieSummary)
                .createdAt(show.getCreatedAt())
                .updatedAt(show.getUpdatedAt())
                .build();

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(showResponse);
    }

}
