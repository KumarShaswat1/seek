package com.example.TicketApp.services;

import com.example.TicketApp.customErrors.BookingNotFoundException;
import com.example.TicketApp.customErrors.UserNotAuthorizedException;
import com.example.TicketApp.customErrors.UserNotFoundException;
import com.example.TicketApp.entity.Booking;
import com.example.TicketApp.entity.User;
import com.example.TicketApp.repository.BookingRespository;
import com.example.TicketApp.repository.UserRespository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BookingService {

    private static final Logger logger = LoggerFactory.getLogger(BookingService.class);

    private final BookingRespository bookingRepository;
    private final UserRespository userRepository;

    @Autowired
    public BookingService(BookingRespository bookingRepository, UserRespository userRepository) {
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
    }

    public boolean validateBooking(long userId, long bookingId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User  not found"));

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found"));

        if (booking.getUser () == null || !booking.getUser ().getUserId().equals(user.getUserId())) {
            throw new UserNotAuthorizedException("User  is not authorized to access this booking");
        }

        logger.info("Booking validation successful for user ID: {} and booking ID: {}", userId, bookingId);
        return true;
    }
}