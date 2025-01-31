package com.example.TicketApp.services;

import com.example.TicketApp.DTO.UserSignRequestDTO;
import com.example.TicketApp.entity.User;
import com.example.TicketApp.repository.UserRespository;
import com.example.TicketApp.constants.ControllerConstants;
import com.example.TicketApp.customErrors.InvalidRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private final UserRespository userRepository;

    @Autowired
    public UserService(UserRespository userRepository) {
        this.userRepository = userRepository;
    }

    public User signup(UserSignRequestDTO userSignRequestDTO) {
        // Check if the user already exists by email
        Optional<User> existingUser  = userRepository.findByEmail(userSignRequestDTO.getEmail());
        if (existingUser .isPresent()) {
            throw new InvalidRequestException("User  with this email already exists");
        }

        // Create and save new user
        User user = new User();
        user.setEmail(userSignRequestDTO.getEmail());
        user.setPassword(userSignRequestDTO.getPassword());  // Save password as plain text (consider hashing in production)
        user.setRole(User.Role.valueOf(userSignRequestDTO.getRole().toUpperCase()));  // Convert role to Enum

        User createdUser  = userRepository.save(user);
        logger.info("User  registered successfully: {}", createdUser .getEmail());
        return createdUser ;
    }
}