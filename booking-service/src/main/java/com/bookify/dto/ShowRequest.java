package com.bookify.dto;

import com.bookify.enums.SeatCategory;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShowRequest {

    @NotNull
    private LocalDateTime showTime;

    @NotNull
    private UUID screenId;

    @NotNull
    private UUID movieId;

    @NotEmpty
    @Size(max = 3)
    private Map<@NotNull SeatCategory, @NotNull @Positive BigDecimal> prices;

}
