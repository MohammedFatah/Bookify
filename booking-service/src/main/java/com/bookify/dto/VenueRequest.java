package com.bookify.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class VenueRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String city;
}
