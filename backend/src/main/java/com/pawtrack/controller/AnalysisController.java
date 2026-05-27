package com.pawtrack.controller;

import com.pawtrack.analysis.AnalysisService;
import com.pawtrack.entity.FeedbackRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AnalysisController {

    private final AnalysisService analysisService;

    // 获取 AI 行为预测推演
    @GetMapping("/behavior/{animalId}")
    public ResponseEntity<Map<String, String>> predictBehavior(@PathVariable Long animalId) {
        String reasoningResult = analysisService.analyzeAnimalBehavior(animalId);
        return ResponseEntity.ok(Map.of("result", reasoningResult));
    }

    // 提交确认/纠错反馈
    @PostMapping("/feedback")
    public ResponseEntity<Void> submitFeedback(@RequestBody FeedbackRequest request) {
        analysisService.saveFeedback(request);
        return ResponseEntity.ok().build();
    }

    // 获取最新的 AI 叙事日记
    @GetMapping("/narrative/{animalId}")
    public ResponseEntity<Map<String, String>> getLatestNarrative(@PathVariable Long animalId) {
        String narrative = analysisService.getLatestNarrative(animalId);
        return ResponseEntity.ok(Map.of("narrative", narrative));
    }
}
