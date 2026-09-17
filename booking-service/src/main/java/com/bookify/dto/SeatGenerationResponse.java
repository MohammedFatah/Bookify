package com.bookify.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatGenerationResponse {
    private UUID screenId;
    private int seatsPerRow;
    private int numberOfRows;
    private int totalSeatsCreated;
}
