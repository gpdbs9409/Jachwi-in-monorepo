import { useEffect, useState, type FormEvent } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { classifyPost } from "../api/llm";
import { createPost, fetchPost, updatePost } from "../api/posts";
import { POST_CATEGORIES, POST_CATEGORY_LABEL, type PostCategory } from "../api/types";

export function PostWritePage() {
  const { postId } = useParams();
  const isEdit = !!postId;
  const navigate = useNavigate();

  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");
  const [category, setCategory] = useState<PostCategory>("ETC");
  const [loading, setLoading] = useState(false);
  const [classifying, setClassifying] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!isEdit) return;
    fetchPost(Number(postId)).then((post) => {
      setTitle(post.title);
      setContent(post.content);
      setCategory(post.category);
    });
  }, [isEdit, postId]);

  const onClassify = async () => {
    if (!title || !content) return;
    setClassifying(true);
    try {
      const suggested = await classifyPost(title, content);
      const match = POST_CATEGORIES.find((c) => suggested.includes(c));
      if (match) setCategory(match);
    } catch (err) {
      console.error("자동 분류 실패", err);
    } finally {
      setClassifying(false);
    }
  };

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      const req = { title, content, category };
      const saved = isEdit ? await updatePost(Number(postId), req) : await createPost(req);
      navigate(`/posts/${saved.id}`);
    } catch {
      setError("게시글 저장에 실패했어요. 로그인이 필요할 수 있어요.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <form className="post-form" onSubmit={onSubmit}>
      <h2>{isEdit ? "게시글 수정" : "게시글 작성"}</h2>
      <label>
        제목
        <input value={title} onChange={(e) => setTitle(e.target.value)} required />
      </label>
      <label>
        내용
        <textarea
          rows={10}
          value={content}
          onChange={(e) => setContent(e.target.value)}
          required
        />
      </label>
      <div className="category-row">
        <label>
          카테고리
          <select value={category} onChange={(e) => setCategory(e.target.value as PostCategory)}>
            {POST_CATEGORIES.map((c) => (
              <option key={c} value={c}>
                {POST_CATEGORY_LABEL[c]}
              </option>
            ))}
          </select>
        </label>
        <button type="button" onClick={onClassify} disabled={classifying}>
          {classifying ? "분류 중..." : "AI로 카테고리 자동분류"}
        </button>
      </div>
      {error && <p className="form-error">{error}</p>}
      <button type="submit" disabled={loading}>
        {loading ? "저장 중..." : "저장"}
      </button>
    </form>
  );
}
