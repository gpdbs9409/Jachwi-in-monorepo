import { mainApi } from "./client";
import type {
  CommentCreateRequest,
  CommentResponse,
  Page,
  PostCategory,
  PostCreateRequest,
  PostResponse,
} from "./types";

export async function fetchPosts(
  category: PostCategory | undefined,
  page: number,
  size = 20
): Promise<Page<PostResponse>> {
  const { data } = await mainApi.get<Page<PostResponse>>("/api/v1/posts", {
    params: { category, page, size },
  });
  return data;
}

export async function fetchPost(postId: number): Promise<PostResponse> {
  const { data } = await mainApi.get<PostResponse>(`/api/v1/posts/${postId}`);
  return data;
}

export async function createPost(req: PostCreateRequest): Promise<PostResponse> {
  const { data } = await mainApi.post<PostResponse>("/api/v1/posts", req);
  return data;
}

export async function updatePost(
  postId: number,
  req: PostCreateRequest
): Promise<PostResponse> {
  const { data } = await mainApi.put<PostResponse>(`/api/v1/posts/${postId}`, req);
  return data;
}

export async function deletePost(postId: number): Promise<void> {
  await mainApi.delete(`/api/v1/posts/${postId}`);
}

export async function fetchComments(postId: number): Promise<CommentResponse[]> {
  const { data } = await mainApi.get<CommentResponse[]>(`/api/v1/posts/${postId}/comments`);
  return data;
}

export async function createComment(
  postId: number,
  req: CommentCreateRequest
): Promise<CommentResponse> {
  const { data } = await mainApi.post<CommentResponse>(
    `/api/v1/posts/${postId}/comments`,
    req
  );
  return data;
}

export async function deleteComment(postId: number, commentId: number): Promise<void> {
  await mainApi.delete(`/api/v1/posts/${postId}/comments/${commentId}`);
}
