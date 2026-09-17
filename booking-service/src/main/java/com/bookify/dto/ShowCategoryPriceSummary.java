package com.bookify.dto;

import com.bookify.enums.SeatCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShowCategoryPriceSummary {
    private SeatCategory seatCategory;
    private BigDecimal price;
}
