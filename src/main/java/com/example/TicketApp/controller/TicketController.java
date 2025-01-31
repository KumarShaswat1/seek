package com.example.TicketApp.controller;

import com.example.TicketApp.DTO.SimpleTicketDTO;
import com.example.TicketApp.DTO.TicketDTO;
import com.example.TicketApp.DTO.TicketResponseDTO;
import com.example.TicketApp.constants.ControllerConstants;
import com.example.TicketApp.entity.Ticket;
import com.example.TicketApp.services.TicketResponseService;
import com.example.TicketApp.services.TicketService;
import com.example.TicketApp.customErrors.InvalidRequestException;
import com.example.TicketApp.customErrors.UserNotFoundException;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin("http://localhost:3000")
@RequestMapping("/ticket")
@AllArgsConstructor
public class TicketController {

    private static final Logger logger = LoggerFactory.getLogger(TicketController.class);

    private TicketService ticketService;
    private TicketResponseService ticketResponseService;

    @Autowired
    public TicketController(TicketResponseService ticketResponseService,TicketService ticketService) {
        this.ticketResponseService = ticketResponseService;
        this.ticketService=ticketService;
    }

    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchTickets(
            @RequestParam long userId,
            @RequestParam String role,
            @RequestParam String status,
            @RequestParam String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Map<String, Object> response = new HashMap<>();
        try {
            Page<SimpleTicketDTO> paginatedTickets = ticketService.getFilteredTickets(userId, role, status, category, page, size);
            response.put("status", ControllerConstants.STATUS_SUCCESS);
            response.put("data", Collections.singletonMap("tickets", paginatedTickets.getContent()));
            response.put("totalElements", paginatedTickets.getTotalElements());
            response.put("totalPages", paginatedTickets.getTotalPages());
            return ResponseEntity.ok(response);
        } catch (UserNotFoundException e) {
            response.put("status", ControllerConstants.STATUS_ERROR);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (InvalidRequestException e) {
            response.put("status", ControllerConstants.STATUS_ERROR);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            logger.error("Error fetching tickets: {}", e.getMessage());
            response.put("status", ControllerConstants.STATUS_ERROR);
            response.put("message", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/search/{userId}/{ticketId}")
    public ResponseEntity<Map<String, Object>> searchTicket(
            @PathVariable long userId,
            @PathVariable long ticketId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Map<String, Object> response = new HashMap<>();

        try {
            TicketDTO ticketDTO = ticketService.searchTicket(userId, ticketId, page, size);
            response.put("status", ControllerConstants.STATUS_SUCCESS);
            response.put("data", ticketDTO);
            return ResponseEntity.ok(response);
        } catch (UserNotFoundException e) {
            response.put("status", ControllerConstants.STATUS_ERROR);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (InvalidRequestException e) {
            response.put("status", ControllerConstants.STATUS_ERROR);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            logger.error("Error fetching ticket: {}", e.getMessage());
            response.put("status", ControllerConstants.STATUS_ERROR);
            response.put("message", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/count/search")
    public ResponseEntity<Map<String, Object>> getTicketCount(@RequestParam long userId,
                                                              @RequestParam String role,
                                                              @RequestParam String category) {
        Map<String, Object> response = new HashMap<>();


        try {
            Map<String, Long> count = ticketService.getCountActiveResolved(userId, role, category);
            response.put("status", ControllerConstants.STATUS_SUCCESS);
            response.put("data", count);
            return ResponseEntity.ok(response);
        } catch (UserNotFoundException e) {
            response.put("status", ControllerConstants.STATUS_ERROR);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (InvalidRequestException e) {
            response.put("status", ControllerConstants.STATUS_ERROR);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            logger.error("Error fetching ticket counts: {}", e.getMessage());
            response.put("status", ControllerConstants.STATUS_ERROR);
            response.put("message", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/{ticket-id}/response")
    public ResponseEntity<?> getAllTicketResponses(@PathVariable("ticket-id") long ticketId,
                                                   @RequestParam long userId) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<TicketResponseDTO> replies = ticketService.getAllTicketResponses(userId, ticketId);
            if (replies.isEmpty()) {
                response.put("status", ControllerConstants.STATUS_ERROR);
                response.put("message", "Ticket not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            response.put("status", ControllerConstants.STATUS_SUCCESS);
            response.put("message", ControllerConstants.MESSAGE_REPLIES_FETCHED);
            response.put("data", Collections.singletonMap("replies", replies));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error fetching ticket responses: {}", e.getMessage());
            response.put("status", ControllerConstants.STATUS_ERROR);
            response.put("message", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping
    public ResponseEntity<?> createTicket(@RequestParam long userId,
                                          @RequestParam String category,
                                          @RequestParam(required = false) String description) {

        Map<String, Object> response = new HashMap<>();
        try {
            if (description == null || description.isEmpty()) {
                description = "No description provided by the user.";
            }

            Ticket createdTicket = ticketService.createTicket(userId, category, description);
            response.put("status", ControllerConstants.STATUS_SUCCESS);
            Map<String, Object> data = new HashMap<>();
            data.put("ticketId", createdTicket.getTicketId());
            data.put("message", ControllerConstants.MESSAGE_TICKET_CREATED);
            response.put("data", data);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (InvalidRequestException e) {
            logger.error("Bad request: {}", e.getMessage());
            response.put("status", ControllerConstants.STATUS_ERROR);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            logger.error("Internal server error: {}", e.getMessage());
            response.put("status", ControllerConstants.STATUS_ERROR);
            response.put("message", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}