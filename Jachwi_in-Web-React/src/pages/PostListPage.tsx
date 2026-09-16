import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { fetchPosts } from "../api/posts";
import { POST_CATEGORIES, POST_CATEGORY_LABEL, type PostCategory, type PostResponse } from "../api/types";

export function PostListPage() {
  const [category, setCategory] = useState<PostCategory | undefined>(undefined);
  const [page, setPage] = useState(0);
  const [posts, setPosts] = useState<PostResponse[]>([]);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);
    fetchPosts(category, page)
      .then((res) => {
        if (cancelled) return;
        setPosts(res.content);
        setTotalPages(res.totalPages);
      })
      .catch(() => !cancelled && setError("게시글을 불러오지 못했어요."))
      .finally(() => !cancelled && setLoading(false));
    return () => {
      cancelled = true;
    };
  }, [category, page]);

  return (
    <div className="post-list-page">
      <div className="post-list-header">
        <div className="category-tabs">
          <button
            className={category === undefined ? "active" : ""}
            onClick={() => {
              setCategory(undefined);
              setPage(0);
            }}
          >
            전체
          </button>
          {POST_CATEGORIES.map((c) => (
            <button
              key={c}
              className={category === c ? "active" : ""}
              onClick={() => {
                setCategory(c);
                setPage(0);
              }}
            >
              {POST_CATEGORY_LABEL[c]}
            </button>
          ))}
        </div>
        <Link to="/posts/new" className="btn primary">글쓰기</Link>
      </div>

      {loading && <p>불러오는 중...</p>}
      {error && <p className="form-error">{error}</p>}

      <ul className="post-list">
        {posts.map((p) => (
          <li key={p.id}>
            <Link to={`/posts/${p.id}`}>
              <span className="post-category">{POST_CATEGORY_LABEL[p.category]}</span>
              <span className="post-title">{p.title}</span>
              <span className="post-meta">
                조회 {p.viewCount} · 댓글 {p.commentCount}
              </span>
            </Link>
          </li>
        ))}
        {!loading && posts.length === 0 && <li className="empty">게시글이 없어요.</li>}
      </ul>

      {totalPages > 1 && (
        <div className="pagination">
          <button disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
            이전
          </button>
          <span>{page + 1} / {totalPages}</span>
          <button disabled={page >= totalPages - 1} onClick={() => setPage((p) => p + 1)}>
            다음
          </button>
        </div>
      )}
    </div>
  );
}
