package com.example.TicketApp.customErrors;

public class TicketResponseNotFoundException extends RuntimeException {
  public TicketResponseNotFoundException(String message) {
    super(message);
  }
}
