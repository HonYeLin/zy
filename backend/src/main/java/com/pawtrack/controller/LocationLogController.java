package com.pawtrack.controller;

import com.pawtrack.entity.LocationLog;
import com.pawtrack.entity.LocationLogCreateRequest;
import com.pawtrack.service.LocationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/locations")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class LocationLogController {

    private final LocationLogService locationLogService;

    @PostMapping
    public ResponseEntity<LocationLog> createLog(@RequestBody LocationLogCreateRequest request) {
        return ResponseEntity.ok(locationLogService.saveLog(request));
    }

    @GetMapping("/animal/{animalId}")
    public ResponseEntity<List<LocationLog>> getLogsByAnimal(@PathVariable Long animalId) {
        return ResponseEntity.ok(locationLogService.findRecentByAnimalId(animalId));
    }

    @GetMapping("/nearby")
    public ResponseEntity<List<LocationLog>> getNearbyLogs(
            @RequestParam Double lat,
            @RequestParam Double lng,
            @RequestParam(defaultValue = "50") Double radius) {
        return ResponseEntity.ok(locationLogService.findWithinRadius(lat, lng, radius));
    }

    @GetMapping("/all")
    public ResponseEntity<List<LocationLog>> getAllLogs() {
        return ResponseEntity.ok(locationLogService.findAllLogs());
    }
}
