package com.bookify.service;

import com.bookify.entity.Movie;
import com.bookify.entity.Screen;
import com.bookify.entity.Seat;
import com.bookify.entity.Show;
import com.bookify.enums.SeatCategory;
import com.bookify.exception.InvalidRequestException;
import com.bookify.exception.ResourceNotFoundException;
import com.bookify.repository.MovieRepository;
import com.bookify.repository.ScreenRepository;
import com.bookify.repository.SeatRepository;
import com.bookify.repository.ShowRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ShowService {

    private final ShowRepository showRepository;
    private final ScreenRepository screenRepository;
    private final MovieRepository movieRepository;
    private final SeatRepository seatRepository;
    private final ShowSeatService showSeatService;
    private final ShowCategoryPriceService showCategoryPriceService;

    public ShowService(
            ShowRepository showRepository,
            ScreenRepository screenRepository,
            MovieRepository movieRepository,
            SeatRepository seatRepository,
            ShowSeatService showSeatService,
            ShowCategoryPriceService showCategoryPriceService) {
        this.showRepository = showRepository;
        this.screenRepository = screenRepository;
        this.movieRepository = movieRepository;
        this.showSeatService = showSeatService;
        this.seatRepository = seatRepository;
        this.showCategoryPriceService = showCategoryPriceService;
    }

    @Transactional
    public Show addShow(Show show, UUID screenId, UUID movieId, Map<SeatCategory, BigDecimal> prices) {
        Screen screen = screenRepository.findById(screenId)
                .orElseThrow(() -> new ResourceNotFoundException("Screen", screenId));

        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new ResourceNotFoundException("Movie", movieId));

        show.setScreen(screen);
        show.setMovie(movie);

        List<Seat> seats = seatRepository.findAllByScreenId(screenId);

        Set<SeatCategory> requiredSeatCategories = seats.stream()
                .map(Seat::getSeatCategory)
                .collect(Collectors.toSet());

        if (!prices.keySet().containsAll(requiredSeatCategories)) {
            requiredSeatCategories.removeAll(prices.keySet());
            throw new InvalidRequestException("Missing prices for seat categories: " + requiredSeatCategories);
        }

        Map<SeatCategory, BigDecimal> requiredShowCategoryPrices = prices.entrySet().stream()
                .filter(entry -> requiredSeatCategories.contains(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        Show savedShow = showRepository.save(show);

        showSeatService.addShowSeats(savedShow, seats);
        showCategoryPriceService.addShowCategoryPrices(savedShow, requiredShowCategoryPrices);

        return savedShow;
    }

    public Show getShow(UUID id) {
        return showRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Show", id));
    }
}
