package com.bookify.service;

import com.bookify.entity.Movie;
import com.bookify.entity.Screen;
import com.bookify.entity.Seat;
import com.bookify.entity.Show;
import com.bookify.exception.ResourceNotFoundException;
import com.bookify.repository.MovieRepository;
import com.bookify.repository.ScreenRepository;
import com.bookify.repository.SeatRepository;
import com.bookify.repository.ShowRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ShowService {

    private final ShowRepository showRepository;
    private final ScreenRepository screenRepository;
    private final MovieRepository movieRepository;
    private final SeatRepository seatRepository;
    private final ShowSeatService showSeatService;

    public ShowService(
            ShowRepository showRepository,
            ScreenRepository screenRepository,
            MovieRepository movieRepository,
            SeatRepository seatRepository,
            ShowSeatService showSeatService) {
        this.showRepository = showRepository;
        this.screenRepository = screenRepository;
        this.movieRepository = movieRepository;
        this.showSeatService = showSeatService;
        this.seatRepository = seatRepository;
    }

    @Transactional
    public Show addShow(Show show, UUID screenId, UUID movieId) {
        Screen screen = screenRepository.findById(screenId)
                .orElseThrow(() -> new ResourceNotFoundException("Screen", screenId));

        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new ResourceNotFoundException("Movie", movieId));

        show.setScreen(screen);
        show.setMovie(movie);

        Show savedShow = showRepository.save(show);

        List<Seat> seats = seatRepository.findAllByScreenId(screenId);
        showSeatService.addShowSeats(savedShow, seats);

        return savedShow;
    }

    public Show getShow(UUID id) {
        return showRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Show", id));
    }
}
