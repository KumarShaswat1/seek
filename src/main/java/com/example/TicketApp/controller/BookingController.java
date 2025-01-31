package com.example.TicketApp.controller;

import com.example.TicketApp.customErrors.BookingNotFoundException;
import com.example.TicketApp.services.BookingService;
import com.example.TicketApp.customErrors.UserNotAuthorizedException;
import com.example.TicketApp.customErrors.UserNotFoundException;
import com.example.TicketApp.constants.ControllerConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@CrossOrigin("http://localhost:3000")
@RequestMapping("/booking")
public class BookingController {

    private static final Logger logger = LoggerFactory.getLogger(BookingController.class);

    private final BookingService bookingService;

    @Autowired
    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping("/{booking-id}/validate")
    public ResponseEntity<Map<String, Object>> validateBooking(
            @RequestParam long userId,
            @PathVariable("booking-id") long bookingId) {

        Map<String, Object> response = new HashMap<>();

        try {
            boolean isValid = bookingService.validateBooking(userId, bookingId);
            response.put("status", ControllerConstants.STATUS_SUCCESS);
            response.put("message", "Booking is valid");
            return ResponseEntity.ok(response);  // 200 OK
        } catch (UserNotFoundException | BookingNotFoundException e) {
            response.put("status", ControllerConstants.STATUS_ERROR);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);  // 404 Not Found
        } catch (UserNotAuthorizedException e) {
            response.put("status", ControllerConstants.STATUS_ERROR);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);  // 403 Forbidden
        } catch (Exception e) {
            logger.error("Error in validateBooking: {}", e.getMessage(), e);
            response.put("status", ControllerConstants.STATUS_ERROR);
            response.put("message", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);  // 500 Internal Server Error
        }
    }
}