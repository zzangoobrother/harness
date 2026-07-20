// 인증 상태 관리: 로그인 여부 / 토큰 / 사용자 정보를 전역으로 제공한다.
// 토큰은 localStorage(api/client.ts)에 저장하고, 파싱된 사용자 정보도 함께 보관하여
// 새로고침 후에도 인증 상태를 복원한다.

import {
  createContext,
  useCallback,
  useContext,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import type { AuthResponse, UserResponse } from "../types/contract";
import { clearToken, getToken, setToken } from "../api/client";

const USER_KEY = "ecommerce.auth.user";

function loadStoredUser(): UserResponse | null {
  const raw = localStorage.getItem(USER_KEY);
  if (!raw) return null;
  try {
    return JSON.parse(raw) as UserResponse;
  } catch {
    // 저장값이 손상된 경우 무시하고 비로그인으로 취급
    localStorage.removeItem(USER_KEY);
    return null;
  }
}

interface AuthContextValue {
  /** 로그인된 사용자 정보. 비로그인 시 null */
  user: UserResponse | null;
  /** 로그인 여부 (토큰 + 사용자 정보가 모두 존재) */
  isAuthenticated: boolean;
  /** 로그인/회원가입 성공 응답(AuthResponse)으로 인증 상태를 확립한다 */
  authenticate: (auth: AuthResponse) => void;
  /** 로그아웃: 토큰/사용자 정보 제거 */
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  // 초기 상태를 localStorage에서 복원 (토큰과 사용자 정보가 모두 있어야 로그인으로 인정)
  const [user, setUser] = useState<UserResponse | null>(() => {
    return getToken() ? loadStoredUser() : null;
  });

  const authenticate = useCallback((auth: AuthResponse) => {
    setToken(auth.token);
    localStorage.setItem(USER_KEY, JSON.stringify(auth.user));
    setUser(auth.user);
  }, []);

  const logout = useCallback(() => {
    clearToken();
    localStorage.removeItem(USER_KEY);
    setUser(null);
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      isAuthenticated: user !== null,
      authenticate,
      logout,
    }),
    [user, authenticate, logout],
  );

  return <AuthContext value={value}>{children}</AuthContext>;
}

// eslint-disable-next-line react-refresh/only-export-components
export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (ctx === undefined) {
    throw new Error("useAuth는 AuthProvider 내부에서만 사용할 수 있습니다.");
  }
  return ctx;
}
