// ── Auth ──────────────────────────────────────────────

export interface LoginRequest {
  email: string;
  password: string;
}

export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
}

export interface JoinRequest {
  email: string;
  name: string;
  nickname: string;
  password: string;
  school: string;
}

export interface UserInfo {
  id: number;
  email: string;
  name: string;
  nickname: string;
  school: string;
}

// ── Map / Building ────────────────────────────────────

export interface Building {
  id: number;
  x: number; // 경도
  y: number; // 위도
  province: string | null;
  district: string | null;
  neighborhood: string | null;
  streetName: string | null;
  buildingMainNo: number | null;
  buildingSubNo: number | null;
  officialBuildingName: string | null;
  detailBuildingName: string | null;
  localBuildingName: string | null;
  busStop: number | null;
  convenienceStore: number | null;
  cafe: number | null;
  streetLight: number | null;
  cctv: number | null;
  hospital: number | null;
  restaurant: number | null;
  schoolDistance: number | null;
}

export interface MapBounds {
  minX: number;
  maxX: number;
  minY: number;
  maxY: number;
}

// ── Community ─────────────────────────────────────────

export const POST_CATEGORIES = [
  "QUESTION",
  "REVIEW",
  "TIP",
  "INFO",
  "ROOMMATE",
  "ETC",
] as const;

export type PostCategory = (typeof POST_CATEGORIES)[number];

export const POST_CATEGORY_LABEL: Record<PostCategory, string> = {
  QUESTION: "질문",
  REVIEW: "후기",
  TIP: "자취팁",
  INFO: "정보공유",
  ROOMMATE: "룸메이트",
  ETC: "기타",
};

export interface PostResponse {
  id: number;
  userId: number;
  title: string;
  content: string;
  category: PostCategory;
  viewCount: number;
  commentCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface PostCreateRequest {
  title: string;
  content: string;
  category: PostCategory;
}

export interface Page<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
  number: number; // 현재 페이지 (0-base)
  size: number;
  last: boolean;
  first: boolean;
}

export interface CommentResponse {
  id: number;
  userId: number;
  parentId: number | null;
  content: string;
  replies: CommentResponse[];
  createdAt: string;
}

export interface CommentCreateRequest {
  content: string;
  parentId?: number | null;
}

// ── LLM ───────────────────────────────────────────────

export interface RoomRecommendRequest {
  school: string;
  budget: number;
  centerX: number;
  centerY: number;
  radius: number;
  preferences: string[];
}

// ── /map 페이지 AI 채팅 추천 ─────────────────────────────

export interface ChatMessage {
  role: "user" | "assistant";
  content: string;
}

export interface ChatRecommendRequest {
  message: string;
  history: ChatMessage[];
  minX: number;
  maxX: number;
  minY: number;
  maxY: number;
}

export interface RecommendedBuilding {
  id: number | null;
  x: number;
  y: number;
  address: string;
  score: number;
  reason: string | null;
}

export interface ChatRecommendResponse {
  reply: string;
  buildings: RecommendedBuilding[];
  usedVectorSearch: boolean;
}
