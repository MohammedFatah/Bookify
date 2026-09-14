package com.bookify.controller;

import com.bookify.dto.MovieRequest;
import com.bookify.dto.MovieResponse;
import com.bookify.entity.Movie;
import com.bookify.service.MovieService;
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

import java.util.UUID;

@RestController
@RequestMapping("/movies")
@RequiredArgsConstructor
public class MovieController {

    private final MovieService movieService;

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
