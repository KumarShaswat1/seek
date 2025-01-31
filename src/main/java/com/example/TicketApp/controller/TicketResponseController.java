package com.example.TicketApp.controller;

import com.example.TicketApp.DTO.TicketResponseDTO;
import com.example.TicketApp.customErrors.UserNotAuthorizedException;
import com.example.TicketApp.entity.TicketResponse;
import com.example.TicketApp.services.TicketResponseService;
import com.example.TicketApp.constants.ControllerConstants;
import com.example.TicketApp.customErrors.InvalidRequestException;
import com.example.TicketApp.customErrors.UnauthorizedAccessException;
import com.example.TicketApp.customErrors.TicketNotFoundException;
import lombok.AllArgsConstructor;
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
@RequestMapping("/ticket-response")
public class TicketResponseController {

    private final TicketResponseService ticketResponseService;
    private static final Logger logger = LoggerFactory.getLogger(TicketResponseController.class);

    @Autowired
    public TicketResponseController(TicketResponseService ticketResponseService) {
        this.ticketResponseService = ticketResponseService;
    }

    // Endpoint to create a new reply (ticket response)
    @PostMapping("/{ticket-id}")
    public ResponseEntity<Map<String, Object>> createTicketResponse(
            @PathVariable("ticket-id") long ticketId,
            @RequestBody Map<String, Object> requestBody) {

        Map<String, Object> response = new HashMap<>();
        try {
            // Extract user_id, role, and replyData from the request body
            long userId = Long.parseLong(requestBody.get("user_id").toString());
            String role = requestBody.get("role").toString();
            Map<String, Object> replyData = (Map<String, Object>) requestBody.get("replyData");

            // Call the service to create the reply
            TicketResponseDTO createdReply = ticketResponseService.createTicketReply(ticketId, userId, role, replyData);

            // Prepare response
            response.put("status", "success");
            response.put("message", "Reply created successfully");
            response.put("data", createdReply);  // Include the DTO in the response

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PutMapping("/{ticket-id}/response/{response-id}")
    public ResponseEntity<?> updateResponse(@PathVariable("ticket-id") long ticketId,
                                            @PathVariable("response-id") long responseId,
                                            @RequestParam long userId,
                                            @RequestBody Map<String, String> updateText) {
        Map<String, Object> response = new HashMap<>();
        try {
            String updatedText = updateText.get("updatedText");
            if (updatedText == null || updatedText.trim().isEmpty()) {
                throw new IllegalArgumentException("Update text cannot be empty");
            }
            ticketResponseService.updateTicketResponse(userId, ticketId, responseId, updatedText);

            response.put("status", "success");
            response.put("message", "Reply updated successfully");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return buildErrorResponse(response, e.getMessage(), HttpStatus.FORBIDDEN);
        } catch (Exception e) {
            return buildErrorResponse(response, "Internal server error", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/{ticket-id}/response/{response-id}")
    public ResponseEntity<?> deleteResponse(@PathVariable("ticket-id") long ticketId,
                                            @PathVariable("response-id") long responseId,
                                            @RequestParam long userId) {
        Map<String, Object> response = new HashMap<>();
        try {
            ticketResponseService.deleteTicketResponse(userId, ticketId, responseId);
            response.put("status", "success");
            response.put("message", "Reply deleted successfully");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return buildErrorResponse(response, e.getMessage(), HttpStatus.FORBIDDEN);
        } catch (Exception e) {
            return buildErrorResponse(response, "Internal server error", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/{ticket-id}/update-status")
    public ResponseEntity<?> updateTicketResponseStatus(@PathVariable("ticket-id") long ticketId,
                                                        @RequestParam long userId) {
        Map<String, Object> response = new HashMap<>();
        try {
            boolean statusUpdated = ticketResponseService.updateTicketResponseStatus(userId, ticketId);
            if (statusUpdated) {
                response.put("status", "success");
                response.put("message", "Status changed successfully");
                return ResponseEntity.ok(response);
            }
            return buildErrorResponse(response, "Ticket not found", HttpStatus.NOT_FOUND);
        } catch (IllegalArgumentException e) {
            return buildErrorResponse(response, e.getMessage(), HttpStatus.FORBIDDEN);
        } catch (Exception e) {
            return buildErrorResponse(response, "Internal server error", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(Map<String, Object> response, String message, HttpStatus status) {
        response.put("status", "error");
        response.put("message", message);
        return ResponseEntity.status(status).body(response);
    }

}