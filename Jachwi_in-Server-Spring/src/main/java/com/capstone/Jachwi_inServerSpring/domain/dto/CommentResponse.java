package com.capstone.Jachwi_inServerSpring.domain.dto;

import com.capstone.Jachwi_inServerSpring.domain.Comment;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class CommentResponse {
    private final Long id;
    private final Long userId;
    private final Long parentId;
    private final String content;
    private final List<CommentResponse> replies;
    private final LocalDateTime createdAt;

    public CommentResponse(Comment comment) {
        this.id        = comment.getId();
        this.userId    = comment.getUserId();
        this.parentId  = comment.getParent() != null ? comment.getParent().getId() : null;
        this.content   = comment.getContent();
        this.replies   = comment.getReplies().stream().map(CommentResponse::new).toList();
        this.createdAt = comment.getCreatedAt();
    }
}
