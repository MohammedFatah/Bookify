package com.bookify.repository;

import com.bookify.entity.Screen;
import com.bookify.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ScreenRepository extends JpaRepository<Screen, UUID> {
    List<Seat> findAllByScreenId(UUID screenId);
}
