package com.pawtrack.dto;

import lombok.Data;

@Data
public class CommentRequest {
    private Long animalId;
    private Long userId;
    private String userNickname;
    private String content;
    private Long parentId;
    private String replyToUserNickname;
}
