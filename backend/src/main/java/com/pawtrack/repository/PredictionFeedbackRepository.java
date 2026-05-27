package com.pawtrack.repository;

import com.pawtrack.entity.PredictionFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PredictionFeedbackRepository extends JpaRepository<PredictionFeedback, Long> {
    
    // 查询某动物历史被人工纠正的记录 (按时间倒序)
    List<PredictionFeedback> findByAnimalIdAndFeedbackTypeOrderByRecordedAtDesc(Long animalId, String feedbackType);
}
