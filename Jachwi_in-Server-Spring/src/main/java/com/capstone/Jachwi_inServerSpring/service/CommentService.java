package com.capstone.Jachwi_inServerSpring.service;

import com.capstone.Jachwi_inServerSpring.client.AuthServerClient;
import com.capstone.Jachwi_inServerSpring.domain.Comment;
import com.capstone.Jachwi_inServerSpring.domain.Post;
import com.capstone.Jachwi_inServerSpring.domain.dto.CommentCreateRequest;
import com.capstone.Jachwi_inServerSpring.domain.dto.CommentResponse;
import com.capstone.Jachwi_inServerSpring.repository.CommentRepository;
import com.capstone.Jachwi_inServerSpring.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final AuthServerClient authServerClient;

    @Transactional(readOnly = true)
    public List<CommentResponse> getComments(Long postId) {
        return commentRepository.findByPostIdAndParentIsNull(postId)
                .stream().map(CommentResponse::new).toList();
    }

    @Transactional
    public CommentResponse createComment(Long postId, CommentCreateRequest req, String email) {
        Long userId = authServerClient.getUserByEmail(email).getId();
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));

        Comment parent = null;
        if (req.getParentId() != null) {
            parent = commentRepository.findById(req.getParentId())
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 댓글입니다."));
        }

        Comment comment = Comment.builder()
                .post(post)
                .userId(userId)
                .parent(parent)
                .content(req.getContent())
                .build();

        return new CommentResponse(commentRepository.save(comment));
    }

    @Transactional
    public void deleteComment(Long commentId, String email) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 댓글입니다."));
        Long userId = authServerClient.getUserByEmail(email).getId();
        if (!comment.getUserId().equals(userId)) {
            throw new IllegalArgumentException("작성자만 삭제할 수 있습니다.");
        }
        commentRepository.delete(comment);
    }
}
