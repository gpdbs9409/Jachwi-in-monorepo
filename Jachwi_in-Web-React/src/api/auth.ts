import { authApi, tokenStorage } from "./client";
import type { JoinRequest, LoginRequest, TokenResponse } from "./types";

export async function login(req: LoginRequest): Promise<TokenResponse> {
  const { data } = await authApi.post<TokenResponse>("/auth/login", req);
  tokenStorage.setTokens(data.accessToken, data.refreshToken);
  return data;
}

export async function sendMailConfirm(email: string): Promise<string> {
  const { data } = await authApi.get<string>(
    `/auth/join/mailConfirm/${encodeURIComponent(email)}`
  );
  return data;
}

export async function verifyMailConfirm(email: string, ePw: string): Promise<boolean> {
  try {
    await authApi.post("/auth/join/mailConfirm", { email, ePw });
    return true;
  } catch {
    return false;
  }
}

export async function join(req: JoinRequest): Promise<string> {
  const { data } = await authApi.post<string>("/auth/join", req);
  return data;
}

export async function logout(): Promise<void> {
  const refreshToken = tokenStorage.getRefreshToken();
  if (refreshToken) {
    try {
      await authApi.post("/auth/logout", null, {
        headers: { Authorization: `Bearer ${refreshToken}` },
      });
    } catch {
      // 서버가 이미 만료 처리했어도 로컬 토큰은 지운다
    }
  }
  tokenStorage.clear();
}
