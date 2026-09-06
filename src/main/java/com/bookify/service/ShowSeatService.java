package com.bookify.service;

import com.bookify.entity.Seat;
import com.bookify.entity.Show;
import com.bookify.entity.ShowSeat;
import com.bookify.enums.SeatStatus;
import com.bookify.exception.ResourceNotFoundException;
import com.bookify.repository.SeatRepository;
import com.bookify.repository.ShowRepository;
import com.bookify.repository.ShowSeatRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ShowSeatService {

    private final ShowSeatRepository showSeatRepository;
    private final ShowRepository showRepository;
    private final SeatRepository seatRepository;

    public ShowSeatService(ShowSeatRepository showSeatRepository, ShowRepository showRepository, SeatRepository seatRepository) {
        this.showSeatRepository = showSeatRepository;
        this.showRepository = showRepository;
        this.seatRepository = seatRepository;
    }

    public void addShowSeats(Show show, List<Seat> seats) {
        for (Seat seat : seats) {
            ShowSeat showSeat = ShowSeat
                    .builder()
                    .show(show)
                    .seat(seat)
                    .seatStatus(SeatStatus.AVAILABLE)
                    .booking(null)
                    .build();

            showSeatRepository.save(showSeat);
        }
    }

}
