package com.example.TicketApp.services;

import com.example.TicketApp.DTO.SimpleTicketDTO;
import com.example.TicketApp.DTO.TicketDTO;
import com.example.TicketApp.DTO.TicketResponseDTO;
import com.example.TicketApp.entity.Ticket;
import com.example.TicketApp.entity.TicketResponse;
import com.example.TicketApp.entity.User;
import com.example.TicketApp.repository.TicketRepository;
import com.example.TicketApp.repository.TicketResponseRepository;
import com.example.TicketApp.repository.UserRespository;
import com.example.TicketApp.constants.ControllerConstants;
import com.example.TicketApp.customErrors.InvalidRequestException;
import com.example.TicketApp.customErrors.UserNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class TicketService {

    private static final Logger logger = LoggerFactory.getLogger(TicketService.class);
    private static long count = 0;

    private final UserRespository userRespository;
    private final TicketRepository ticketRepository;
    private final TicketResponseRepository ticketResponseRepository;

    @Autowired
    public TicketService(UserRespository userRespository, TicketRepository ticketRepository, TicketResponseRepository ticketResponseRepository) {
        this.userRespository = userRespository;
        this.ticketRepository = ticketRepository;
        this.ticketResponseRepository = ticketResponseRepository;
    }

    public Page<SimpleTicketDTO> getFilteredTickets(long userId, String role, String status, String category, int page, int size) {
        User user = userRespository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User  not found with ID: " + userId));

        List<Ticket> tickets;

        if ("AGENT".equalsIgnoreCase(role)) {
            tickets = user.getTicketsAsAgent();
        } else if ("CUSTOMER".equalsIgnoreCase(role)) {
            tickets = user.getTicketsAsCustomer();
        } else {
            throw new IllegalArgumentException("Invalid role. Must be 'AGENT' or 'CUSTOMER'.");
        }

        List<SimpleTicketDTO> filteredTickets = tickets.stream()
                .filter(ticket -> filterTickets(ticket, status, category))
                .map(ticket -> new SimpleTicketDTO(
                        ticket.getTicketId(),
                        ticket.getDescription(),
                        ticket.getStatus().name(),
                        ticket.getCategory().name(),
                        ticket.getCreatedAt(),
                        ticket.getUpdatedAt(),
                        ticket.getAgent() != null ? ticket.getAgent().getEmail() : null,
                        ticket.getCustomer() != null ? ticket.getCustomer().getEmail() : null
                ))
                .collect(Collectors.toList());

        int start = page * size;
        int end = Math.min(start + size, filteredTickets.size());

        if (start >= filteredTickets.size()) {
            return new PageImpl<>(Collections.emptyList(), PageRequest.of(page, size), filteredTickets.size());
        }

        List<SimpleTicketDTO> paginatedList = filteredTickets.subList(start, end);
        return new PageImpl<>(paginatedList, PageRequest.of(page, size), filteredTickets.size());
    }

    private boolean filterTickets(Ticket ticket, String status, String category) {
        return ("ALL".equalsIgnoreCase(status) || status.equalsIgnoreCase(ticket.getStatus().name())) &&
                ("ALL".equalsIgnoreCase(category) || category.equalsIgnoreCase(ticket.getCategory().name()));
    }

    public Map<String, Long> getCountActiveResolved(long userId, String role, String category) {
        if (role == null || (!role.equalsIgnoreCase("AGENT") && !role.equalsIgnoreCase("CUSTOMER"))) {
            throw new IllegalArgumentException("Invalid role. Role must be 'AGENT' or 'CUSTOMER'.");
        }

        List<Ticket> tickets = ticketRepository.findAll().stream()
                .filter(ticket -> {
                    if (role.equalsIgnoreCase("AGENT")) {
                        return ticket.getAgent() != null && ticket.getAgent().getUserId() == userId;
                    } else {
                        return ticket.getCustomer() != null && ticket.getCustomer().getUserId() == userId;
                    }
                })
                .collect(Collectors.toList());

        long activeCount = tickets.stream()
                .filter(ticket -> filterTicketsCategory(ticket, "ACTIVE", category))
                .count();

        long resolvedCount = tickets.stream()
                .filter(ticket -> filterTicketsCategory(ticket, "RESOLVED", category))
                .count();

        Map<String, Long> count = new HashMap<>();
        count.put("Active_tickets", activeCount);
        count.put("Resolved_tickets", resolvedCount);

        return count;
    }

    private boolean filterTicketsCategory(Ticket ticket, String status, String category) {
        return ("ALL".equalsIgnoreCase(status) || status.equalsIgnoreCase(ticket.getStatus().name())) &&
                ("ALL".equalsIgnoreCase(category) || category.equalsIgnoreCase(ticket.getCategory().name()));
    }

    public TicketDTO searchTicket(long userId, long ticketId, int page, int size) {
        User user = userRespository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User  not found with ID: " + userId));

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found with ID: " + ticketId));

        if (!ticket.getCustomer().equals(user) && (ticket.getAgent() == null || !ticket.getAgent().equals(user))) {
            throw new InvalidRequestException("User  ID " + userId + " is not authorized to view ticket ID " + ticketId);
        }

        List<TicketResponse> responses = Optional.ofNullable(ticket.getResponses())
                .orElse(Collections.emptyList());

        if (page < 0 || size <= 0 || page * size >= responses.size()) {
            return new TicketDTO(ticket.getTicketId(), ticket.getDescription(), ticket.getStatus().name(),
                    ticket.getCategory().name(), ticket.getCreatedAt(), ticket.getUpdatedAt(), Collections.emptyList());
        }

        int start = page * size;
        int end = Math.min(start + size, responses.size());
        List<TicketResponse> paginatedResponses = responses.subList(start, end);

        List<TicketResponseDTO> responseDTOs = new ArrayList<>();
        for (TicketResponse response : paginatedResponses) {
            responseDTOs.add(new TicketResponseDTO(
                    response.getResponseId(),
                    ticket.getTicketId(),
                    response.getResponseText(),
                    response.getRole() != null ? response.getRole().toString() : "UNKNOWN",
                    response.getUser () != null ? response.getUser ().getEmail() : "No Email",
                    ticket.getAgent() != null ? ticket.getAgent().getEmail() : ticket.getCustomer().getEmail(),
                    response.getCreatedAt()
            ));
        }

        return new TicketDTO(
                ticket.getTicketId(),
                ticket.getDescription(),
                ticket.getStatus().name(),
                ticket.getCategory().name(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt(),
                responseDTOs
        );
    }

    public List<TicketResponseDTO> getAllTicketResponses(long userId, long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found with ID: " + ticketId));

        List<TicketResponse> ticketResponses = ticket.getResponses();

        List<TicketResponseDTO> repliesDTO = new ArrayList<>();
        for (TicketResponse ticketResponse : ticketResponses) {
            User user = ticketResponse.getUser ();

            TicketResponseDTO responseDTO = new TicketResponseDTO(
                    ticketResponse.getResponseId(),
                    ticket.getTicketId(),
                    ticketResponse.getResponseText(),
                    ticketResponse.getRole().toString(),
                    user != null ? user.getEmail() : "No Email",
                    ticket.getAgent() != null ? ticket.getAgent().getEmail() : null,
                    ticketResponse.getCreatedAt()
            );

            repliesDTO.add(responseDTO);
        }

        return repliesDTO;
    }

    public Ticket createTicket(long userId, String category, String description) {
        logger.info("Starting to create ticket for userId: " + userId + " with category: " + category);

        try {
            User customer = userRespository.findById(userId)
                    .orElseThrow(() -> new UserNotFoundException("User  not found"));

            if (!customer.getRole().equals(User.Role.CUSTOMER)) {
                throw new InvalidRequestException("Only customers can create tickets");
            }

            if (!category.equalsIgnoreCase("prebooking") && !category.equalsIgnoreCase("postbooking")) {
                throw new InvalidRequestException("Invalid category. Must be 'prebooking' or 'postbooking'");
            }

            Ticket ticket = new Ticket();
            ticket.setCustomer(customer);
            ticket.setCategory(Ticket.Category.valueOf(category.toUpperCase()));
            ticket.setStatus(Ticket.Status.ACTIVE);
            ticket.setDescription(description);

            User agent = assignAgentToTicket();
            ticket.setAgent(agent);

            ticket = ticketRepository.save(ticket);
            logger.info("Ticket created with ID: " + ticket.getTicketId());

            return ticket;
        } catch (IllegalArgumentException e) {
            logger.error("Argument exception occurred: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error: " + e.getMessage());
            throw new RuntimeException("Internal server error while creating ticket ", e);
        }
    }

    private User assignAgentToTicket() {
        List<User> agents = userRespository.findByRole(User.Role.AGENT);

        if (agents.isEmpty()) {
            logger.error("No available agents for ticket assignment");
            throw new IllegalStateException("No available agents for ticket assignment");
        }

        User assignedAgent = agents.get((int) (count++ % agents.size()));
        logger.info("Assigned agent: " + assignedAgent.getEmail());
        return assignedAgent;
    }
}
