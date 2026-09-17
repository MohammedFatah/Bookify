package com.bookify.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShowSummary {
    private UUID id;
    private LocalDateTime showTime;
    private ScreenSummary screenSummary;
    private MovieSummary movieSummary;
}
