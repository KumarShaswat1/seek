package com.example.TicketApp.constants;

public class ControllerConstants {
    // General response statuses
    public static final String STATUS_SUCCESS = "success";
    public static final String STATUS_ERROR = "error";

    // User-related messages
    public static final String MESSAGE_USER_REGISTERED = "User  registered successfully";
    public static final String MESSAGE_USER_NOT_FOUND = "User  not found";
    public static final String MESSAGE_INVALID_CREDENTIALS = "Invalid username or password";
    public static final String MESSAGE_LOGIN_SUCCESSFUL = "Login successful";

    // Ticket-related messages
    public static final String MESSAGE_TICKET_CREATED = "Ticket created successfully";
    public static final String MESSAGE_TICKET_NOT_FOUND = "Ticket not found";
    public static final String MESSAGE_REPLIES_FETCHED = "Replies fetched successfully";
    public static final String MESSAGE_STATUS_UPDATED = "Status changed successfully";

    // Ticket response messages
    public static final String MESSAGE_REPLY_CREATED = "Reply created successfully";
    public static final String MESSAGE_REPLY_UPDATED = "Reply updated successfully";
    public static final String MESSAGE_REPLY_DELETED = "Reply deleted successfully";

    // Error messages
    public static final String MESSAGE_INTERNAL_SERVER_ERROR = "Internal server error";
    public static final String MESSAGE_UPDATE_TEXT_EMPTY = "Update text cannot be empty";
}