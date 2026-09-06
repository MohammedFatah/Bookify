package com.bookify.controller;

import com.bookify.dto.ScreenSummary;
import com.bookify.dto.SeatRequest;
import com.bookify.dto.SeatResponse;
import com.bookify.entity.Seat;
import com.bookify.service.SeatService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/seats")
public class SeatController {

    private final SeatService seatService;

    public SeatController(SeatService seatService) {
        this.seatService = seatService;
    }

    @PostMapping
    public ResponseEntity<SeatResponse> addSeat(@Valid @RequestBody SeatRequest seatRequest) {
        Seat seat = Seat.builder()
                .rowId(seatRequest.getRowId())
                .number(seatRequest.getNumber())
                .seatCategory(seatRequest.getSeatCategory())
                .build();

        Seat savedSeat = seatService.addSeat(seat, seatRequest.getScreenId());

        ScreenSummary screenSummary = ScreenSummary
                .builder()
                .id(savedSeat.getScreen().getId())
                .name(savedSeat.getScreen().getName())
                .type(savedSeat.getScreen().getType())
                .build();

        SeatResponse seatResponse = SeatResponse
                .builder()
                .id(savedSeat.getId())
                .rowId(savedSeat.getRowId())
                .number(savedSeat.getNumber())
                .seatCategory(savedSeat.getSeatCategory())
                .screenSummary(screenSummary)
                .createdAt(savedSeat.getCreatedAt())
                .updatedAt(savedSeat.getUpdatedAt())
                .build();

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(seatResponse);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SeatResponse> getSeat(@PathVariable UUID id) {
        if (id == null) {
            return ResponseEntity.badRequest().build();
        }

        Seat seat = seatService.getSeat(id);

        ScreenSummary screenSummary = ScreenSummary
                .builder()
                .id(seat.getScreen().getId())
                .name(seat.getScreen().getName())
                .type(seat.getScreen().getType())
                .build();

        SeatResponse seatResponse = SeatResponse
                .builder()
                .id(seat.getId())
                .rowId(seat.getRowId())
                .number(seat.getNumber())
                .screenSummary(screenSummary)
                .seatCategory(seat.getSeatCategory())
                .createdAt(seat.getCreatedAt())
                .updatedAt(seat.getUpdatedAt())
                .build();

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(seatResponse);
    }

}
