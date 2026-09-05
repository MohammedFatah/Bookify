package com.bookify.dto;

import com.bookify.enums.ScreenType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScreenRequest {

    @NotBlank
    private String name;

    @NotBlank
    private ScreenType type;

    @NotNull
    @Positive
    private Short capacity;

    @NotBlank
    private UUID venueId;

}
