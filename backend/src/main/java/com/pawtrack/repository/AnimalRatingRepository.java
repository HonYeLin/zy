package com.pawtrack.repository;

import com.pawtrack.entity.AnimalRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AnimalRatingRepository extends JpaRepository<AnimalRating, Long> {
    List<AnimalRating> findByAnimalId(Long animalId);
}
