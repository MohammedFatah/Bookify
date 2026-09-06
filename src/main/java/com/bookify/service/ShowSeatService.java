package com.bookify.service;

import com.bookify.entity.Seat;
import com.bookify.entity.Show;
import com.bookify.entity.ShowSeat;
import com.bookify.enums.SeatStatus;
import com.bookify.repository.SeatRepository;
import com.bookify.repository.ShowRepository;
import com.bookify.repository.ShowSeatRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ShowSeatService {

    private final ShowSeatRepository showSeatRepository;

    public ShowSeatService(ShowSeatRepository showSeatRepository, ShowRepository showRepository, SeatRepository seatRepository) {
        this.showSeatRepository = showSeatRepository;
    }

    public void addShowSeats(Show show, List<Seat> seats) {
        List<ShowSeat> showSeats = seats.stream()
                .map(seat -> ShowSeat
                        .builder()
                        .show(show)
                        .seat(seat)
                        .seatStatus(SeatStatus.AVAILABLE)
                        .booking(null)
                        .build())
                .toList();

        showSeatRepository.saveAll(showSeats);
    }

}
