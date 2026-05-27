package com.pawtrack.repository;

import com.pawtrack.entity.AnimalLifeNarrative;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnimalLifeNarrativeRepository extends JpaRepository<AnimalLifeNarrative, Long> {
    
    // 获取某个动物最新的生活日记叙事
    List<AnimalLifeNarrative> findByAnimalIdOrderByStartTimeDesc(Long animalId);
}
