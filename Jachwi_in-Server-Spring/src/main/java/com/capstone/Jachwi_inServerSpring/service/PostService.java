package com.capstone.Jachwi_inServerSpring.service;

import com.capstone.Jachwi_inServerSpring.client.AuthServerClient;
import com.capstone.Jachwi_inServerSpring.domain.Post;
import com.capstone.Jachwi_inServerSpring.domain.PostCategory;
import com.capstone.Jachwi_inServerSpring.domain.dto.PostCreateRequest;
import com.capstone.Jachwi_inServerSpring.domain.dto.PostResponse;
import com.capstone.Jachwi_inServerSpring.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final AuthServerClient authServerClient;

    @Transactional(readOnly = true)
    public Page<PostResponse> getPosts(PostCategory category, Pageable pageable) {
        Page<Post> posts = (category != null)
                ? postRepository.findByCategory(category, pageable)
                : postRepository.findAll(pageable);
        return posts.map(PostResponse::new);
    }

    @Transactional
    public PostResponse getPost(Long postId) {
        Post post = findPost(postId);
        post.increaseViewCount();
        return new PostResponse(post);
    }

    @Transactional
    public PostResponse createPost(PostCreateRequest req, String email) {
        Long userId = authServerClient.getUserByEmail(email).getId();
        Post post = Post.builder()
                .userId(userId)
                .title(req.getTitle())
                .content(req.getContent())
                .category(req.getCategory())
                .build();
        return new PostResponse(postRepository.save(post));
    }

    @Transactional
    public PostResponse updatePost(Long postId, PostCreateRequest req, String email) {
        Post post = findPost(postId);
        validateAuthor(post, email);
        post.update(req.getTitle(), req.getContent(), req.getCategory());
        return new PostResponse(post);
    }

    @Transactional
    public void deletePost(Long postId, String email) {
        Post post = findPost(postId);
        validateAuthor(post, email);
        postRepository.delete(post);
    }

    private Post findPost(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));
    }

    private void validateAuthor(Post post, String email) {
        Long userId = authServerClient.getUserByEmail(email).getId();
        if (!post.getUserId().equals(userId)) {
            throw new IllegalArgumentException("작성자만 수정/삭제할 수 있습니다.");
        }
    }
}
