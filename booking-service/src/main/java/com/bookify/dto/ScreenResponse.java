package com.bookify.dto;

import com.bookify.entity.Venue;
import com.bookify.enums.ScreenType;
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
public class ScreenResponse {
    private UUID id;
    private String name;
    private ScreenType type;
    private Short capacity;
    private VenueSummary venue;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
