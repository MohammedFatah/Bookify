package com.bookify.dto;

import com.bookify.enums.SeatStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShowSeatSummary {
    private SeatSummary seatSummary;
    private SeatStatus seatStatus;
}
