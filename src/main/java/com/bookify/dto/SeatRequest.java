package com.bookify.dto;

import com.bookify.entity.Screen;
import com.bookify.enums.SeatCategory;
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
@AllArgsConstructor
@NoArgsConstructor
public class SeatRequest {

    @NotBlank
    private String rowId;

    @NotNull
    @Positive
    private Short number;

    @NotNull
    private UUID screenId;

    @NotNull
    private SeatCategory seatCategory;
}
