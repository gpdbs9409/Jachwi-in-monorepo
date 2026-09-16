package com.capstone.Jachwi_inServerSpring.controller;

import com.capstone.Jachwi_inServerSpring.domain.PostCategory;
import com.capstone.Jachwi_inServerSpring.domain.dto.CommentCreateRequest;
import com.capstone.Jachwi_inServerSpring.domain.dto.CommentResponse;
import com.capstone.Jachwi_inServerSpring.domain.dto.PostCreateRequest;
import com.capstone.Jachwi_inServerSpring.domain.dto.PostResponse;
import com.capstone.Jachwi_inServerSpring.service.CommentService;
import com.capstone.Jachwi_inServerSpring.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/posts")
public class PostController {

    private final PostService postService;
    private final CommentService commentService;

    // 게시글 목록 (카테고리 필터, 페이징)
    // GET /api/v1/posts?category=REVIEW&page=0&size=20
    @GetMapping
    public ResponseEntity<Page<PostResponse>> getPosts(
            @RequestParam(required = false) PostCategory category,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(postService.getPosts(category, pageable));
    }

    // 게시글 상세 (조회수 +1)
    @GetMapping("/{postId}")
    public ResponseEntity<PostResponse> getPost(@PathVariable Long postId) {
        return ResponseEntity.ok(postService.getPost(postId));
    }

    // 게시글 작성
    @PostMapping
    public ResponseEntity<PostResponse> createPost(@RequestBody PostCreateRequest req,
                                                   Authentication auth) {
        return ResponseEntity.ok(postService.createPost(req, auth.getName()));
    }

    // 게시글 수정
    @PutMapping("/{postId}")
    public ResponseEntity<PostResponse> updatePost(@PathVariable Long postId,
                                                   @RequestBody PostCreateRequest req,
                                                   Authentication auth) {
        return ResponseEntity.ok(postService.updatePost(postId, req, auth.getName()));
    }

    // 게시글 삭제
    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(@PathVariable Long postId,
                                           Authentication auth) {
        postService.deletePost(postId, auth.getName());
        return ResponseEntity.noContent().build();
    }

    // ── 댓글 ──────────────────────────────────────

    // 댓글 목록
    @GetMapping("/{postId}/comments")
    public ResponseEntity<List<CommentResponse>> getComments(@PathVariable Long postId) {
        return ResponseEntity.ok(commentService.getComments(postId));
    }

    // 댓글 작성
    @PostMapping("/{postId}/comments")
    public ResponseEntity<CommentResponse> createComment(@PathVariable Long postId,
                                                         @RequestBody CommentCreateRequest req,
                                                         Authentication auth) {
        return ResponseEntity.ok(commentService.createComment(postId, req, auth.getName()));
    }

    // 댓글 삭제
    @DeleteMapping("/{postId}/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long postId,
                                              @PathVariable Long commentId,
                                              Authentication auth) {
        commentService.deleteComment(commentId, auth.getName());
        return ResponseEntity.noContent().build();
    }
}
