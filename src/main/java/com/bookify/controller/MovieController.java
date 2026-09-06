package com.bookify.controller;

import com.bookify.dto.MovieRequest;
import com.bookify.dto.MovieResponse;
import com.bookify.entity.Movie;
import com.bookify.service.MovieService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/movies")
public class MovieController {

    private final MovieService movieService;

    public MovieController(MovieService movieService) {
        this.movieService = movieService;
    }

    @PostMapping
    public ResponseEntity<MovieResponse> addMovie(@Valid @RequestBody MovieRequest movieRequest) {
        Movie movie = Movie
                .builder()
                .title(movieRequest.getTitle())
                .durationMinutes(movieRequest.getDurationMinutes())
                .build();

        Movie savedMovie = movieService.addMovie(movie);

        MovieResponse movieResponse = MovieResponse
                .builder()
                .id(savedMovie.getId())
                .title(savedMovie.getTitle())
                .durationMinutes(savedMovie.getDurationMinutes())
                .createdAt(savedMovie.getCreatedAt())
                .updatedAt(savedMovie.getUpdatedAt())
                .build();

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(movieResponse);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MovieResponse> getMovie(@PathVariable UUID id) {
        if (id == null) {
            return ResponseEntity.badRequest().build();
        }

        Movie movie = movieService.getMovie(id);

        MovieResponse movieResponse = MovieResponse
                .builder()
                .id(movie.getId())
                .title(movie.getTitle())
                .durationMinutes(movie.getDurationMinutes())
                .createdAt(movie.getCreatedAt())
                .updatedAt(movie.getUpdatedAt())
                .build();

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(movieResponse);
    }
}
