package com.pawtrack.service;

import com.pawtrack.entity.User;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AdminAuthService {

    private final Map<String, User> tokenStore = new ConcurrentHashMap<>();

    public String createToken(User adminUser) {
        String token = UUID.randomUUID().toString();
        tokenStore.put(token, adminUser);
        return token;
    }

    public User getAdminByToken(String token) {
        if (token == null || !tokenStore.containsKey(token)) {
            return null;
        }
        return tokenStore.get(token);
    }
    
    public void logout(String token) {
        if (token != null) {
            tokenStore.remove(token);
        }
    }
}
