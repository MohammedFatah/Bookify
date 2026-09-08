package com.bookify.repository;

import com.bookify.entity.ShowSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ShowSeatRepository extends JpaRepository<ShowSeat, UUID> {

}
