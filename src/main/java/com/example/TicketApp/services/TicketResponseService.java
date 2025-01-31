package com.example.TicketApp.services;

import com.example.TicketApp.DTO.TicketResponseDTO;
import com.example.TicketApp.entity.Ticket;
import com.example.TicketApp.entity.TicketResponse;
import com.example.TicketApp.entity.User;
import com.example.TicketApp.repository.TicketRepository;
import com.example.TicketApp.repository.TicketResponseRepository;
import com.example.TicketApp.repository.UserRespository;
import com.example.TicketApp.constants.ControllerConstants;
import com.example.TicketApp.customErrors.InvalidRequestException;
import com.example.TicketApp.customErrors.UnauthorizedAccessException;
import com.example.TicketApp.customErrors.TicketNotFoundException;
import com.example.TicketApp.customErrors.UserNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class TicketResponseService {

    private static final Logger logger = LoggerFactory.getLogger(TicketResponseService.class);

    private final TicketRepository ticketRepository;
    private final TicketResponseRepository ticketResponseRepository;
    private final UserRespository userRespository;

    @Autowired
    public TicketResponseService(TicketRepository ticketRepository,
                                 TicketResponseRepository ticketResponseRepository,
                                 UserRespository userRespository) {
        this.ticketRepository = ticketRepository;
        this.ticketResponseRepository = ticketResponseRepository;
        this.userRespository = userRespository;
    }

    public TicketResponseDTO createTicketReply(long ticketId, long userId, String role, Map<String, Object> replyData) {
        validateRole(role);
        validateReplyData(replyData);

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException("Ticket not found with ID: " + ticketId));

        User user = userRespository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User  not found with ID: " + userId));

        validateAuthorization(ticket, user, role);

        TicketResponse ticketResponse = new TicketResponse();
        ticketResponse.setTicket(ticket);
        ticketResponse.setUser (user);
        ticketResponse.setRole(TicketResponse.Role.valueOf(role.toUpperCase()));
        ticketResponse.setResponseText(replyData.get("responseText").toString());

        TicketResponse savedResponse = ticketResponseRepository.save(ticketResponse);
        ticket.getResponses().add(savedResponse);
        ticketRepository.save(ticket);

        logger.info("Ticket response created successfully for ticket ID: {}", ticketId);

        return mapToDTO(savedResponse, ticket, user);
    }

    private void validateReplyData(Map<String, Object> replyData) {
        if (replyData == null || !replyData.containsKey("responseText") || replyData.get("responseText") == null) {
            throw new InvalidRequestException("Reply data must include a non-null 'responseText' field.");
        }
    }

    private TicketResponseDTO mapToDTO(TicketResponse savedResponse, Ticket ticket, User user) {
        return new TicketResponseDTO(
                savedResponse.getResponseId(),
                ticket.getTicketId(),
                savedResponse.getResponseText(),
                savedResponse.getRole().toString(),
                user.getEmail(),
                (ticket.getAgent() != null) ? ticket.getAgent().getEmail() : null,
                savedResponse.getCreatedAt()
        );
    }
    private void validateRole(String role) {
        if (role == null || (!role.equalsIgnoreCase("AGENT") && !role.equalsIgnoreCase("CUSTOMER"))) {
            throw new InvalidRequestException("Invalid role. Must be 'AGENT' or 'CUSTOMER'.");
        }
    }

    private void validateAuthorization(Ticket ticket, User user, String role) {
        if (role.equalsIgnoreCase("AGENT")) {
            if (ticket.getAgent() == null || !ticket.getAgent().equals(user)) {
                throw new UnauthorizedAccessException("User  is not authorized to perform this action on the ticket.");
            }
        } else if (role.equalsIgnoreCase("CUSTOMER")) {
            if (!ticket.getCustomer().equals(user)) {
                throw new UnauthorizedAccessException("User  is not authorized to perform this action on the ticket.");
            }
        }
    }

    public TicketResponse updateTicketResponse(long userId, long ticketId, long responseId, String updateText) {
        User user = userRespository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User  not found"));

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException("Ticket not found"));

        TicketResponse ticketResponse = ticketResponseRepository.findById(responseId)
                .orElseThrow(() -> new TicketNotFoundException("Reply not found"));

        if (!ticketResponse.getUser ().equals(user)) {
            throw new UnauthorizedAccessException("User  is not authorized to update this reply");
        }

        ticketResponse.setResponseText(updateText);
        logger.info("Ticket response updated successfully for response ID: {}", responseId);
        return ticketResponseRepository.save(ticketResponse);
    }

    public void deleteTicketResponse(long userId, long ticketId, long responseId) {
        User user = userRespository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User  not found"));

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException("Ticket not found"));

        TicketResponse ticketResponse = ticketResponseRepository.findById(responseId)
                .orElseThrow(() -> new TicketNotFoundException("Reply not found"));

        if (!ticketResponse.getUser ().equals(user)) {
            throw new UnauthorizedAccessException("User  is not authorized to delete this reply");
        }

        ticketResponseRepository.delete(ticketResponse);
        logger.info("Ticket response deleted successfully for response ID: {}", responseId);
    }

    public boolean updateTicketResponseStatus(long userId, long ticketId) {
        User user = userRespository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User  not found"));

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException("Ticket not found"));

        // Logic to update the ticket response status
        // This could involve checking the current status and updating it accordingly

        logger.info("Ticket response status updated successfully for ticket ID: {}", ticketId);
        return true; // Return true if the update was successful
    }
}