package com.pawtrack.repository;

import com.pawtrack.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByGuestDeviceId(String guestDeviceId);

    @Query("SELECT COUNT(u) FROM User u WHERE u.role = 'GUEST'")
    long countGuests();
}
