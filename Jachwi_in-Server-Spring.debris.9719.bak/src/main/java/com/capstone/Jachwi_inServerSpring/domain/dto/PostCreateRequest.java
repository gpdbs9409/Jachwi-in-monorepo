package com.capstone.Jachwi_inServerSpring.domain.dto;

import com.capstone.Jachwi_inServerSpring.domain.PostCategory;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PostCreateRequest {
    private String title;
    private String content;
    private PostCategory category;
}
