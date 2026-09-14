package com.bookify.service;

import com.bookify.entity.User;
import com.bookify.exception.ResourceAlreadyExistsException;
import com.bookify.exception.ResourceNotFoundException;
import com.bookify.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public User addUser(User user) {
        boolean emailAlreadyExists = user.getEmail() != null &&
                userRepository.existsByEmail(user.getEmail());

        boolean phoneNumberAlreadyExists = userRepository.existsByPhoneNumber(user.getPhoneNumber());

        List<String> conflicts = new ArrayList<>();
        if (emailAlreadyExists) conflicts.add("email: " + user.getEmail());
        if (phoneNumberAlreadyExists) conflicts.add("phone number: " + user.getPhoneNumber());

        if (!conflicts.isEmpty()) {
            throw new ResourceAlreadyExistsException("User already exists with " + String.join(" and ", conflicts));
        }

        return userRepository.save(user);
    }

    public User getUser(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

}
