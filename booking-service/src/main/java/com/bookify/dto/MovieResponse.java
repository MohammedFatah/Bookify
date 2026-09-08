package com.bookify.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MovieResponse {
    private UUID id;
    private String title;
    private Short durationMinutes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
