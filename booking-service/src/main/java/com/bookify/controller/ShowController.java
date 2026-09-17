package com.bookify.controller;

import com.bookify.dto.MovieSummary;
import com.bookify.dto.ScreenSummary;
import com.bookify.dto.ShowCategoryPriceSummary;
import com.bookify.dto.ShowRequest;
import com.bookify.dto.ShowResponse;
import com.bookify.dto.ShowSeatSummary;
import com.bookify.dto.ShowSeatsResponse;
import com.bookify.entity.Show;
import com.bookify.service.ShowCategoryPriceService;
import com.bookify.service.ShowService;
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
@RequestMapping("/shows")
@RequiredArgsConstructor
public class ShowController {

    private final ShowService showService;
    private final ShowCategoryPriceService showCategoryPriceService;

    @GetMapping("/{showId}/seats")
    public ResponseEntity<ShowSeatsResponse> getAvailableSeatsForShow(@PathVariable UUID showId) {
        List<ShowSeatSummary> allSeatsForShow = showService.getAllSeatsForShow(showId);

        ShowSeatsResponse showSeatsResponse = ShowSeatsResponse.builder()
                .showId(showId)
                .seats(allSeatsForShow)
                .build();

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(showSeatsResponse);
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
