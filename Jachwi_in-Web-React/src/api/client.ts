import axios, { type InternalAxiosRequestConfig } from "axios";

const AUTH_BASE_URL = import.meta.env.VITE_AUTH_API_URL ?? "http://localhost:8081";
const MAIN_BASE_URL = import.meta.env.VITE_MAIN_API_URL ?? "http://localhost:8080";

const ACCESS_TOKEN_KEY = "jachwiin_access_token";
const REFRESH_TOKEN_KEY = "jachwiin_refresh_token";

export const tokenStorage = {
  getAccessToken: () => localStorage.getItem(ACCESS_TOKEN_KEY),
  getRefreshToken: () => localStorage.getItem(REFRESH_TOKEN_KEY),
  setTokens: (accessToken: string, refreshToken: string) => {
    localStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
    localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
  },
  setAccessToken: (accessToken: string) => {
    localStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
  },
  clear: () => {
    localStorage.removeItem(ACCESS_TOKEN_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
  },
};

export const authApi = axios.create({ baseURL: AUTH_BASE_URL });
export const mainApi = axios.create({ baseURL: MAIN_BASE_URL });

function attachAuthHeader(config: InternalAxiosRequestConfig) {
  const token = tokenStorage.getAccessToken();
  if (token) {
    config.headers.set("Authorization", `Bearer ${token}`);
  }
  return config;
}

authApi.interceptors.request.use(attachAuthHeader);
mainApi.interceptors.request.use(attachAuthHeader);

// mainApi 요청이 401(만료된 accessToken)을 받으면 refreshToken으로 한 번만 재시도한다.
let refreshPromise: Promise<string> | null = null;

async function refreshAccessToken(): Promise<string> {
  const refreshToken = tokenStorage.getRefreshToken();
  if (!refreshToken) throw new Error("no refresh token");

  if (!refreshPromise) {
    refreshPromise = authApi
      .post<string>("/auth/refresh", null, {
        headers: { Authorization: `Bearer ${refreshToken}` },
      })
      .then((res) => {
        tokenStorage.setAccessToken(res.data);
        return res.data;
      })
      .finally(() => {
        refreshPromise = null;
      });
  }
  return refreshPromise;
}

mainApi.interceptors.response.use(
  (res) => res,
  async (error) => {
    const original = error.config;
    if (error.response?.status === 401 && !original._retry && tokenStorage.getRefreshToken()) {
      original._retry = true;
      try {
        const newToken = await refreshAccessToken();
        original.headers.Authorization = `Bearer ${newToken}`;
        return mainApi(original);
      } catch {
        tokenStorage.clear();
      }
    }
    return Promise.reject(error);
  }
);

/** JWT는 서버 서명 검증 없이, 화면 표시용으로 subject(email)만 가볍게 꺼내 쓴다. */
export function decodeEmailFromToken(token: string): string | null {
  try {
    const payload = token.split(".")[1];
    const json = JSON.parse(atob(payload.replace(/-/g, "+").replace(/_/g, "/")));
    return json.sub ?? null;
  } catch {
    return null;
  }
}
