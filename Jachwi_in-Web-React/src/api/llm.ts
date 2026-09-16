import { mainApi } from "./client";
import type {
  RoomRecommendRequest,
  ChatRecommendRequest,
  ChatRecommendResponse,
} from "./types";

export async function classifyPost(title: string, content: string): Promise<string> {
  const { data } = await mainApi.post<string>("/api/v1/llm/classify", { title, content });
  return data;
}

/** @deprecated 수동 폼 기반 추천. /map 페이지의 채팅 추천(chatRecommend)으로 대체됨 — 하위호환용으로만 유지. */
export async function recommendRooms(req: RoomRecommendRequest): Promise<string> {
  const { data } = await mainApi.post<string>("/api/v1/llm/recommend", req);
  return data;
}

/** /map 페이지에 붙는 자연어 채팅 추천. 현재 지도 뷰포트를 검색 범위로 사용한다. */
export async function chatRecommend(req: ChatRecommendRequest): Promise<ChatRecommendResponse> {
  const { data } = await mainApi.post<ChatRecommendResponse>("/api/v1/llm/chat", req);
  return data;
}
