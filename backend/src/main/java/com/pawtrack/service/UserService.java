package com.pawtrack.service;

import com.pawtrack.entity.User;
import com.pawtrack.repository.UserRepository;
import com.pawtrack.util.PasswordUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public User register(String username, String password, String nickname) {
        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("用户名和密码不能为空");
        }
        if (nickname == null || nickname.trim().isEmpty()) {
            nickname = username;
        }
        
        Optional<User> existingUser = userRepository.findByUsername(username.trim());
        if (existingUser.isPresent()) {
            throw new IllegalArgumentException("用户名已存在");
        }

        User user = new User();
        user.setUsername(username.trim());
        user.setNickname(nickname.trim());
        user.setRole("USER");

        String salt = PasswordUtil.generateSalt();
        String hashedPassword = PasswordUtil.hashPassword(password, salt);
        user.setSalt(salt);
        user.setPassword(hashedPassword);

        return userRepository.save(user);
    }

    public User login(String username, String password) {
        if (username == null || password == null) {
            throw new IllegalArgumentException("用户名和密码不能为空");
        }
        
        Optional<User> userOpt = userRepository.findByUsername(username.trim());
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("用户名或密码错误");
        }

        User user = userOpt.get();
        if ("GUEST".equals(user.getRole())) {
            throw new IllegalArgumentException("不可登录游客账号");
        }

        boolean matched = PasswordUtil.verifyPassword(password, user.getSalt(), user.getPassword());
        if (!matched) {
            throw new IllegalArgumentException("用户名或密码错误");
        }

        return user;
    }

    public User getOrCreateGuest(String guestDeviceId) {
        if (guestDeviceId == null || guestDeviceId.trim().isEmpty()) {
            throw new IllegalArgumentException("设备ID不能为空");
        }

        Optional<User> guestOpt = userRepository.findByGuestDeviceId(guestDeviceId.trim());
        if (guestOpt.isPresent()) {
            return guestOpt.get();
        }

        // Generate next sequential guest nickname
        long guestCount = userRepository.countGuests();
        String guestNickname = "游客" + (guestCount + 1);

        User guest = new User();
        guest.setNickname(guestNickname);
        guest.setRole("GUEST");
        guest.setGuestDeviceId(guestDeviceId.trim());

        return userRepository.save(guest);
    }
}
