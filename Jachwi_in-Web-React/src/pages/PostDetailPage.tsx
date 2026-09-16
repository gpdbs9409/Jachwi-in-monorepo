import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { useAuth } from "../contexts/AuthContext";
import {
  createComment,
  deleteComment,
  deletePost,
  fetchComments,
  fetchPost,
} from "../api/posts";
import { POST_CATEGORY_LABEL, type CommentResponse, type PostResponse } from "../api/types";

function CommentItem({
  comment,
  postId,
  onReply,
  onDelete,
}: {
  comment: CommentResponse;
  postId: number;
  onReply: (parentId: number, content: string) => Promise<void>;
  onDelete: (commentId: number) => Promise<void>;
}) {
  const [replying, setReplying] = useState(false);
  const [replyText, setReplyText] = useState("");

  return (
    <li className="comment-item">
      <p>{comment.content}</p>
      <div className="comment-actions">
        <button onClick={() => setReplying((v) => !v)}>답글</button>
        <button onClick={() => onDelete(comment.id)}>삭제</button>
      </div>
      {replying && (
        <form
          className="reply-form"
          onSubmit={async (e) => {
            e.preventDefault();
            await onReply(comment.id, replyText);
            setReplyText("");
            setReplying(false);
          }}
        >
          <input
            value={replyText}
            onChange={(e) => setReplyText(e.target.value)}
            placeholder="답글을 입력하세요"
            required
          />
          <button type="submit">등록</button>
        </form>
      )}
      {comment.replies.length > 0 && (
        <ul className="comment-replies">
          {comment.replies.map((r) => (
            <CommentItem key={r.id} comment={r} postId={postId} onReply={onReply} onDelete={onDelete} />
          ))}
        </ul>
      )}
    </li>
  );
}

export function PostDetailPage() {
  const { postId } = useParams();
  const id = Number(postId);
  const navigate = useNavigate();
  const { isAuthenticated } = useAuth();

  const [post, setPost] = useState<PostResponse | null>(null);
  const [comments, setComments] = useState<CommentResponse[]>([]);
  const [newComment, setNewComment] = useState("");
  const [error, setError] = useState<string | null>(null);

  const reload = () => {
    fetchPost(id).then(setPost);
    fetchComments(id).then(setComments);
  };

  useEffect(reload, [id]);

  const onDeletePost = async () => {
    if (!confirm("게시글을 삭제할까요?")) return;
    try {
      await deletePost(id);
      navigate("/posts");
    } catch {
      setError("삭제에 실패했어요. 작성자만 삭제할 수 있어요.");
    }
  };

  const onSubmitComment = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await createComment(id, { content: newComment });
      setNewComment("");
      reload();
    } catch {
      setError("댓글 작성에 실패했어요. 로그인이 필요해요.");
    }
  };

  const onReply = async (parentId: number, content: string) => {
    await createComment(id, { content, parentId });
    reload();
  };

  const onDeleteComment = async (commentId: number) => {
    await deleteComment(id, commentId);
    reload();
  };

  if (!post) return <p>불러오는 중...</p>;

  return (
    <div className="post-detail-page">
      <span className="post-category">{POST_CATEGORY_LABEL[post.category]}</span>
      <h2>{post.title}</h2>
      <p className="post-meta">
        조회 {post.viewCount} · 댓글 {post.commentCount}
      </p>
      <p className="post-content">{post.content}</p>

      <div className="post-actions">
        <Link to={`/posts/${post.id}/edit`}>수정</Link>
        <button onClick={onDeletePost}>삭제</button>
      </div>

      {error && <p className="form-error">{error}</p>}

      <h3>댓글</h3>
      {isAuthenticated ? (
        <form className="comment-form" onSubmit={onSubmitComment}>
          <input
            value={newComment}
            onChange={(e) => setNewComment(e.target.value)}
            placeholder="댓글을 입력하세요"
            required
          />
          <button type="submit">등록</button>
        </form>
      ) : (
        <p>
          댓글을 작성하려면 <Link to="/login">로그인</Link>이 필요해요.
        </p>
      )}
      <ul className="comment-list">
        {comments.map((c) => (
          <CommentItem key={c.id} comment={c} postId={id} onReply={onReply} onDelete={onDeleteComment} />
        ))}
      </ul>
    </div>
  );
}
