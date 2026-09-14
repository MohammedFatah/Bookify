package com.bookify.controller;

import com.bookify.dto.*;
import com.bookify.entity.Show;
import com.bookify.service.ShowCategoryPriceService;
import com.bookify.service.ShowService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/shows")
public class ShowController {

    private final ShowService showService;
    private final ShowCategoryPriceService showCategoryPriceService;

    public ShowController(ShowService showService, ShowCategoryPriceService showCategoryPriceService) {
        this.showService = showService;
        this.showCategoryPriceService = showCategoryPriceService;
    }

    @PostMapping
    public ResponseEntity<ShowResponse> addShow(@Valid @RequestBody ShowRequest showRequest) {
        Show show = Show.builder()
                .showTime(showRequest.getShowTime())
                .build();

        Show savedShow = showService.addShow(show, showRequest.getScreenId(), showRequest.getMovieId(), showRequest.getPrices());

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

        List<ShowCategoryPriceSummary> prices = showRequest.getPrices().entrySet().stream()
                .map(entry -> ShowCategoryPriceSummary
                        .builder()
                        .seatCategory(entry.getKey())
                        .price(entry.getValue())
                        .build())
                .toList();

        ShowResponse showResponse = ShowResponse
                .builder()
                .id(savedShow.getId())
                .showTime(savedShow.getShowTime())
                .screenSummary(screenSummary)
                .movieSummary(movieSummary)
                .prices(prices)
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

        List<ShowCategoryPriceSummary> prices = showCategoryPriceService.getShowCategoryPrices(id).stream()
                .map(showCategoryPrice -> ShowCategoryPriceSummary
                        .builder()
                        .seatCategory(showCategoryPrice.getSeatCategory())
                        .price(showCategoryPrice.getPrice())
                        .build())
                .toList();

        ShowResponse showResponse = ShowResponse
                .builder()
                .id(show.getId())
                .showTime(show.getShowTime())
                .screenSummary(screenSummary)
                .movieSummary(movieSummary)
                .prices(prices)
                .createdAt(show.getCreatedAt())
                .updatedAt(show.getUpdatedAt())
                .build();

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(showResponse);
    }

}
