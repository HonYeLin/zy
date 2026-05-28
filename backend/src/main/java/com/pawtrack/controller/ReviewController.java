package com.pawtrack.controller;

import com.pawtrack.dto.CommentRequest;
import com.pawtrack.dto.RatingRequest;
import com.pawtrack.dto.RatingStatsDTO;
import com.pawtrack.entity.AnimalComment;
import com.pawtrack.entity.AnimalRating;
import com.pawtrack.service.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reviews")
@CrossOrigin(origins = "*")
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    @PostMapping("/comments")
    public ResponseEntity<AnimalComment> addComment(@RequestBody CommentRequest request) {
        return ResponseEntity.ok(reviewService.addComment(request));
    }

    @GetMapping("/comments/next-guest-id")
    public ResponseEntity<java.util.Map<String, String>> getNextGuestId() {
        java.util.Map<String, String> response = new java.util.HashMap<>();
        response.put("nickname", reviewService.getNextGuestNickname());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/comments/{animalId}")
    public ResponseEntity<Page<AnimalComment>> getComments(
            @PathVariable Long animalId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(defaultValue = "createdAt") String sort) {
        
        // sort expected values: "createdAt" or "likeCount"
        Sort sortOrder = Sort.by(Sort.Direction.DESC, sort);
        Pageable pageable = PageRequest.of(page, size, sortOrder);
        
        return ResponseEntity.ok(reviewService.getCommentsByAnimalId(animalId, pageable));
    }

    @PostMapping("/comments/{commentId}/like")
    public ResponseEntity<AnimalComment> likeComment(
            @PathVariable Long commentId,
            @RequestParam(defaultValue = "false") boolean cancel) {
        AnimalComment comment = reviewService.likeComment(commentId, cancel);
        if (comment != null) {
            return ResponseEntity.ok(comment);
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long commentId,
            @RequestParam Long userId) {
        boolean success = reviewService.deleteComment(commentId, userId);
        if (success) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.status(403).build();
    }

    @PostMapping("/ratings")
    public ResponseEntity<AnimalRating> addRating(@RequestBody RatingRequest request) {
        return ResponseEntity.ok(reviewService.addRating(request));
    }

    @GetMapping("/ratings/{animalId}/stats")
    public ResponseEntity<RatingStatsDTO> getRatingStats(@PathVariable Long animalId) {
        return ResponseEntity.ok(reviewService.getRatingStats(animalId));
    }
}
