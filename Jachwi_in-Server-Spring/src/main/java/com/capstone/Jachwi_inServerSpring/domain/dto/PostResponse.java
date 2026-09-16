package com.capstone.Jachwi_inServerSpring.domain.dto;

import com.capstone.Jachwi_inServerSpring.domain.Post;
import com.capstone.Jachwi_inServerSpring.domain.PostCategory;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class PostResponse {
    private final Long id;
    private final Long userId;
    private final String title;
    private final String content;
    private final PostCategory category;
    private final int viewCount;
    private final int commentCount;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public PostResponse(Post post) {
        this.id           = post.getId();
        this.userId       = post.getUserId();
        this.title        = post.getTitle();
        this.content      = post.getContent();
        this.category     = post.getCategory();
        this.viewCount    = post.getViewCount();
        this.commentCount = post.getComments().size();
        this.createdAt    = post.getCreatedAt();
        this.updatedAt    = post.getUpdatedAt();
    }
}
