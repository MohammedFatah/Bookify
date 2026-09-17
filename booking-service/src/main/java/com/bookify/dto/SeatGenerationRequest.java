package com.bookify.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SeatGenerationRequest {

    @NotNull
    @Positive
    private Short seatsPerRow;

}
