package com.pawtrack.controller.admin;

import com.pawtrack.entity.User;
import com.pawtrack.service.AdminAuthService;
import com.pawtrack.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/auth")
@CrossOrigin(origins = "*")
public class AdminAuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private AdminAuthService adminAuthService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        try {
            String username = request.get("username");
            String password = request.get("password");
            User user = userService.login(username, password);

            if (!"ADMIN".equals(user.getRole())) {
                return ResponseEntity.status(403).body(createErrorResponse("Not an admin account"));
            }

            String token = adminAuthService.createToken(user);
            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("username", user.getUsername());
            response.put("nickname", user.getNickname());
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }
    
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            adminAuthService.logout(token);
        }
        return ResponseEntity.ok().build();
    }

    private Map<String, String> createErrorResponse(String message) {
        Map<String, String> error = new HashMap<>();
        error.put("error", message);
        return error;
    }
}
