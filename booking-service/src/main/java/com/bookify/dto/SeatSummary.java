package com.bookify.dto;

import com.bookify.enums.SeatCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatSummary {
    private UUID seatId;
    private String rowId;
    private Short number;
    private SeatCategory seatCategory;
    private BigDecimal price;
}
