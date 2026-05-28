package com.pawtrack.controller;

import com.pawtrack.entity.User;
import com.pawtrack.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> request) {
        try {
            String username = request.get("username");
            String password = request.get("password");
            String nickname = request.get("nickname");
            User user = userService.register(username, password, nickname);
            return ResponseEntity.ok(cleanUser(user));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        try {
            String username = request.get("username");
            String password = request.get("password");
            User user = userService.login(username, password);
            return ResponseEntity.ok(cleanUser(user));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/guest")
    public ResponseEntity<?> getOrCreateGuest(@RequestBody Map<String, String> request) {
        try {
            String guestDeviceId = request.get("guestDeviceId");
            User guest = userService.getOrCreateGuest(guestDeviceId);
            return ResponseEntity.ok(cleanUser(guest));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    private User cleanUser(User user) {
        if (user != null) {
            user.setPassword(null);
            user.setSalt(null);
        }
        return user;
    }

    private Map<String, String> createErrorResponse(String message) {
        Map<String, String> error = new HashMap<>();
        error.put("error", message);
        return error;
    }
}
