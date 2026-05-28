package com.pawtrack.controller.admin;

import com.pawtrack.entity.AnimalComment;
import com.pawtrack.repository.AnimalCommentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/comments")
@CrossOrigin(origins = "*")
public class AdminCommentController {

    @Autowired
    private AnimalCommentRepository commentRepository;

    @GetMapping
    public ResponseEntity<List<AnimalComment>> getAllComments() {
        return ResponseEntity.ok(commentRepository.findAll());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteComment(@PathVariable Long id) {
        return commentRepository.findById(id).map(comment -> {
            commentRepository.delete(comment);
            return ResponseEntity.ok().build();
        }).orElse(ResponseEntity.notFound().build());
    }
}
