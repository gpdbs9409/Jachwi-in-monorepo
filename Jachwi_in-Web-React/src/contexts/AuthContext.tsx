import { createContext, useContext, useEffect, useState, type ReactNode } from "react";
import { decodeEmailFromToken, tokenStorage } from "../api/client";
import * as authApi from "../api/auth";
import type { JoinRequest, LoginRequest } from "../api/types";

interface AuthContextValue {
  email: string | null;
  isAuthenticated: boolean;
  login: (req: LoginRequest) => Promise<void>;
  logout: () => Promise<void>;
  join: (req: JoinRequest) => Promise<string>;
  sendMailConfirm: (email: string) => Promise<string>;
  verifyMailConfirm: (email: string, ePw: string) => Promise<boolean>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [email, setEmail] = useState<string | null>(() => {
    const token = tokenStorage.getAccessToken();
    return token ? decodeEmailFromToken(token) : null;
  });

  useEffect(() => {
    // 다른 탭에서 로그인/로그아웃한 경우 동기화
    const onStorage = () => {
      const token = tokenStorage.getAccessToken();
      setEmail(token ? decodeEmailFromToken(token) : null);
    };
    window.addEventListener("storage", onStorage);
    return () => window.removeEventListener("storage", onStorage);
  }, []);

  const value: AuthContextValue = {
    email,
    isAuthenticated: !!email,
    login: async (req) => {
      const { accessToken } = await authApi.login(req);
      setEmail(decodeEmailFromToken(accessToken));
    },
    logout: async () => {
      await authApi.logout();
      setEmail(null);
    },
    join: authApi.join,
    sendMailConfirm: authApi.sendMailConfirm,
    verifyMailConfirm: authApi.verifyMailConfirm,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}
