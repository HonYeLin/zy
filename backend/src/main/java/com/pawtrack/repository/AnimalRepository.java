package com.pawtrack.repository;

import com.pawtrack.entity.Animal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AnimalRepository extends JpaRepository<Animal, Long> {
    List<Animal> findByNameAndBreed(String name, String breed);
    List<Animal> findByBreed(String breed);
    Optional<Animal> findByQrCodeId(String qrCodeId);
}
