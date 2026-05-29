package com.pawtrack.service;

import com.pawtrack.dto.CommentRequest;
import com.pawtrack.dto.RatingRequest;
import com.pawtrack.dto.RatingStatsDTO;
import com.pawtrack.entity.AnimalComment;
import com.pawtrack.entity.AnimalRating;
import com.pawtrack.repository.AnimalCommentRepository;
import com.pawtrack.repository.AnimalRatingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ReviewService {

    @Autowired
    private AnimalCommentRepository commentRepository;

    @Autowired
    private AnimalRatingRepository ratingRepository;

    public AnimalComment addComment(CommentRequest request) {
        AnimalComment comment = new AnimalComment();
        comment.setAnimalId(request.getAnimalId());
        comment.setUserId(request.getUserId());
        comment.setUserNickname(request.getUserNickname() == null || request.getUserNickname().trim().isEmpty() ? "匿名用户" : request.getUserNickname());
        comment.setContent(request.getContent());
        comment.setParentId(request.getParentId());
        comment.setReplyToUserNickname(request.getReplyToUserNickname());
        comment.setLikeCount(0);
        return commentRepository.save(comment);
    }

    public String getNextGuestNickname() {
        List<String> nicknames = commentRepository.findGuestNicknames();
        long maxId = 0;
        for (String name : nicknames) {
            if (name != null && name.startsWith("游客")) {
                try {
                    String numStr = name.substring(2).trim();
                    long id = Long.parseLong(numStr);
                    if (id > maxId) {
                        maxId = id;
                    }
                } catch (NumberFormatException e) {
                    // Ignore non-numeric suffixes
                }
            }
        }
        return "游客" + (maxId + 1);
    }

    public Page<AnimalComment> getCommentsByAnimalId(Long animalId, Pageable pageable) {
        return commentRepository.findByAnimalIdAndParentIdIsNull(animalId, pageable);
    }

    public Page<AnimalComment> getRepliesByParentId(Long parentId, Pageable pageable) {
        return commentRepository.findByParentId(parentId, pageable);
    }

    @org.springframework.transaction.annotation.Transactional
    public boolean deleteComment(Long commentId, Long userId) {
        Optional<AnimalComment> commentOpt = commentRepository.findById(commentId);
        if (commentOpt.isPresent()) {
            AnimalComment comment = commentOpt.get();
            if (comment.getUserId() != null && comment.getUserId().equals(userId)) {
                if (comment.getParentId() == null) {
                    commentRepository.deleteByParentId(commentId);
                }
                commentRepository.delete(comment);
                return true;
            }
        }
        return false;
    }

    public AnimalComment likeComment(Long commentId, boolean cancel) {
        Optional<AnimalComment> commentOpt = commentRepository.findById(commentId);
        if (commentOpt.isPresent()) {
            AnimalComment comment = commentOpt.get();
            if (cancel) {
                comment.setLikeCount(Math.max(0, comment.getLikeCount() - 1));
            } else {
                comment.setLikeCount(comment.getLikeCount() + 1);
            }
            return commentRepository.save(comment);
        }
        return null;
    }

    public AnimalRating addRating(RatingRequest request) {
        AnimalRating rating = null;
        if (request.getUserId() != null) {
            rating = ratingRepository.findByAnimalIdAndUserId(request.getAnimalId(), request.getUserId());
        }
        if (rating == null) {
            rating = new AnimalRating();
            rating.setAnimalId(request.getAnimalId());
            rating.setUserId(request.getUserId());
        }
        rating.setAppearanceScore(request.getAppearanceScore());
        rating.setTemperScore(request.getTemperScore());
        rating.setVisibilityScore(request.getVisibilityScore());
        rating.setClinginessScore(request.getClinginessScore());
        return ratingRepository.save(rating);
    }

    public RatingStatsDTO getRatingStats(Long animalId) {
        List<AnimalRating> ratings = ratingRepository.findByAnimalId(animalId);
        RatingStatsDTO stats = new RatingStatsDTO();
        
        if (ratings.isEmpty()) {
            stats.setAppearanceAvg(0.0);
            stats.setTemperAvg(0.0);
            stats.setVisibilityAvg(0.0);
            stats.setClinginessAvg(0.0);
            stats.setTotalRatings(0);
            return stats;
        }

        double appearanceSum = 0;
        double temperSum = 0;
        double visibilitySum = 0;
        double clinginessSum = 0;

        for (AnimalRating r : ratings) {
            appearanceSum += r.getAppearanceScore();
            temperSum += r.getTemperScore();
            visibilitySum += r.getVisibilityScore();
            clinginessSum += r.getClinginessScore();
        }

        int count = ratings.size();
        stats.setAppearanceAvg(appearanceSum / count);
        stats.setTemperAvg(temperSum / count);
        stats.setVisibilityAvg(visibilitySum / count);
        stats.setClinginessAvg(clinginessSum / count);
        stats.setTotalRatings(count);

        return stats;
    }
}
