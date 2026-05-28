package com.pawtrack.controller.admin;

import com.pawtrack.entity.Animal;
import com.pawtrack.repository.AnimalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RestController
@RequestMapping("/api/admin/animals")
@CrossOrigin(origins = "*")
public class AdminAnimalController {

    @Autowired
    private AnimalRepository animalRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping
    public ResponseEntity<List<Animal>> getAllAnimals() {
        return ResponseEntity.ok(animalRepository.findAll());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateAnimal(@PathVariable Long id, @RequestBody Animal animalDetails) {
        return animalRepository.findById(id).map(animal -> {
            if (animalDetails.getName() != null) animal.setName(animalDetails.getName());
            if (animalDetails.getBreed() != null) animal.setBreed(animalDetails.getBreed());
            if (animalDetails.getAvatarUrl() != null) animal.setAvatarUrl(animalDetails.getAvatarUrl());
            if (animalDetails.getDescription() != null) animal.setDescription(animalDetails.getDescription());
            if (animalDetails.getAiSummary() != null) animal.setAiSummary(animalDetails.getAiSummary());
            animalRepository.save(animal);
            return ResponseEntity.ok().build();
        }).orElse(ResponseEntity.notFound().build());
    }

    @Transactional
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAnimal(@PathVariable Long id) {
        return animalRepository.findById(id).map(animal -> {
            jdbcTemplate.update("DELETE FROM animal_logs WHERE animal_id = ?", id);
            jdbcTemplate.update("DELETE FROM animal_life_narratives WHERE animal_id = ?", id);
            jdbcTemplate.update("DELETE FROM animal_comments WHERE animal_id = ?", id);
            jdbcTemplate.update("DELETE FROM animal_ratings WHERE animal_id = ?", id);
            jdbcTemplate.update("DELETE FROM ai_predictions WHERE animal_id = ?", id);
            jdbcTemplate.update("DELETE FROM prediction_feedbacks WHERE animal_id = ?", id);
            
            animalRepository.delete(animal);
            return ResponseEntity.ok().build();
        }).orElse(ResponseEntity.notFound().build());
    }
}
