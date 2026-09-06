package com.bookify.dto;

import com.bookify.enums.ScreenType;
import com.bookify.enums.SeatCategory;
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
public class SeatResponse {
    private UUID id;
    private String rowId;
    private Short number;
    private ScreenSummary screenSummary;
    private SeatCategory seatCategory;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
