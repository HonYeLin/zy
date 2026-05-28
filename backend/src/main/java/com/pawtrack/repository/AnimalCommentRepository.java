package com.pawtrack.repository;

import com.pawtrack.entity.AnimalComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AnimalCommentRepository extends JpaRepository<AnimalComment, Long> {
    Page<AnimalComment> findByAnimalId(Long animalId, Pageable pageable);

    @Query(value = "SELECT user_nickname FROM animal_comments WHERE user_nickname LIKE '游客%'", nativeQuery = true)
    List<String> findGuestNicknames();
}
