package com.example.TicketApp.controller;

import com.example.TicketApp.DTO.UserSignRequestDTO;
import com.example.TicketApp.entity.User;
import com.example.TicketApp.repository.UserRespository;
import com.example.TicketApp.services.UserService;
import com.example.TicketApp.constants.ControllerConstants;
import com.example.TicketApp.customErrors.InvalidRequestException;
import com.example.TicketApp.customErrors.UserNotFoundException;
import com.example.TicketApp.customErrors.UnauthorizedAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@CrossOrigin("http://localhost:3000")
public class UserController {

    private final UserService userService;
    private final UserRespository userRespository;
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    public UserController(UserService userService, UserRespository userRespository) {
        this.userService = userService;
        this.userRespository = userRespository;
    }

    @PostMapping("/signup")
    public ResponseEntity<Map<String, Object>> signup(@RequestBody UserSignRequestDTO userSignRequestDTO) {
        Map<String, Object> response = new HashMap<>();
        try {
            User createdUser  = userService.signup(userSignRequestDTO);
            response.put("status", ControllerConstants.STATUS_SUCCESS);
            response.put("message", ControllerConstants.MESSAGE_USER_REGISTERED);
            response.put("data", createdUser );

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (InvalidRequestException e) {
            response.put("status", ControllerConstants.STATUS_ERROR);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);  // 400 Bad Request
        } catch (Exception e) {
            logger.error("Error in signup: {}", e.getMessage(), e);
            response.put("status", ControllerConstants.STATUS_ERROR);
            response.put("message", ControllerConstants.MESSAGE_INTERNAL_SERVER_ERROR);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);  // 500 Internal Server Error
        }
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody UserSignRequestDTO userSignRequestDTO) {
        Map<String, Object> response = new HashMap<>();

        try {
            Optional<User> existingUser  = userRespository.findByEmail(userSignRequestDTO.getEmail());
            if (!existingUser .isPresent()) {
                throw new UserNotFoundException(ControllerConstants.MESSAGE_USER_NOT_FOUND);
            }

            User user = existingUser .get();

            if (!user.getPassword().equals(userSignRequestDTO.getPassword())) {
                throw new UnauthorizedAccessException(ControllerConstants.MESSAGE_INVALID_CREDENTIALS);
            }

            response.put("status", ControllerConstants.STATUS_SUCCESS);
            response.put("message", ControllerConstants.MESSAGE_LOGIN_SUCCESSFUL);

            Map<String, Object> userData = new HashMap<>();
            userData.put("user_id", user.getUserId());
            userData.put("email", user.getEmail());
            userData.put("role", user.getRole());

            response.put("data", userData);

            return ResponseEntity.ok(response); // 200 OK

        } catch (UserNotFoundException e) {
            response.put("status", ControllerConstants.STATUS_ERROR);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response); // 404 Not Found
        } catch (UnauthorizedAccessException e) {
            response.put("status", ControllerConstants.STATUS_ERROR);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response); // 401 Unauthorized
        } catch (Exception e) {
            logger.error("Error in login: {}", e.getMessage(), e);
            response.put("status", ControllerConstants.STATUS_ERROR);
            response.put("message", ControllerConstants.MESSAGE_INTERNAL_SERVER_ERROR);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response); // 500 Internal Server Error
        }
    }
}