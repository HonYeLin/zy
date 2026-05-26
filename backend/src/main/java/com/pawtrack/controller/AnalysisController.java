package com.pawtrack.controller;

import com.pawtrack.analysis.AnalysisService;
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

    @GetMapping("/behavior/{animalId}")
    public ResponseEntity<Map<String, String>> predictBehavior(@PathVariable Long animalId) {
        String reasoningResult = analysisService.analyzeAnimalBehavior(animalId);
        return ResponseEntity.ok(Map.of("result", reasoningResult));
    }
}
