package com.bookify.service;

import com.bookify.entity.Screen;
import com.bookify.repository.ScreenRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class ScreenService {

    private final ScreenRepository screenRepository;

    public ScreenService(ScreenRepository screenRepository) {
        this.screenRepository = screenRepository;
    }

    public Screen addScreen(Screen screen) {
        return screenRepository.save(screen);
    }

    public Optional<Screen> getScreen(UUID id) {
        return screenRepository.findById(id);
    }

}
