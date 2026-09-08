package com.bookify.dto;

import com.bookify.enums.ScreenType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScreenSummary {
    private UUID id;
    private String name;
    private ScreenType type;
}
