package com.bookify.dto;

import jakarta.validation.constraints.NotNull;
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
public class ShowRequest {

    @NotNull
    private LocalDateTime showTime;

    @NotNull
    private UUID screenId;

    @NotNull
    private UUID movieId;
}
