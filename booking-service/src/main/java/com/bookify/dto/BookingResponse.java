package com.bookify.dto;

import com.bookify.entity.Show;
import com.bookify.entity.User;
import com.bookify.enums.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BookingResponse {
    private UUID id;
    private UserSummary userSummary;
    private ShowSummary showSummary;
    private BookingStatus bookingStatus;
    private BigDecimal totalAmount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
