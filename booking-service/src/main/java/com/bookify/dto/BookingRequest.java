package com.bookify.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
public class BookingRequest {

    @NotNull
    private UUID userId;

    @NotNull
    private UUID showId;

}
