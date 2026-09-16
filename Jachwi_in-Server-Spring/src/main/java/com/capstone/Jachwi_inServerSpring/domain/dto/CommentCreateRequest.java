package com.capstone.Jachwi_inServerSpring.domain.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CommentCreateRequest {
    private String content;
    private Long parentId; // null이면 최상위 댓글, 값이 있으면 대댓글
}
