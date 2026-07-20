// API 클라이언트: axios 인스턴스 + JWT 자동 첨부 + 공통 에러 처리.
// 계약(api-spec.md)의 표준 에러 스키마 `ApiErrorResponse`를 그대로 소비한다.

import axios, { AxiosError, type AxiosInstance } from "axios";
import type { ApiErrorResponse } from "../types/contract";

// ----------------------------------------------------------------------------
// 토큰 저장소 (localStorage)
// ----------------------------------------------------------------------------
// Why localStorage: 새로고침 후에도 로그인 유지가 필요한 MVP 요구사항 때문.
// (sessionStorage는 탭 종료 시 소실, 쿠키는 CORS/CSRF 추가 설정 필요.)
// XSS 위험은 존재하므로 프로덕션 확장 시 httpOnly 쿠키 전환을 검토한다.

const TOKEN_KEY = "ecommerce.auth.token";

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY);
}

export function setToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token);
}

export function clearToken(): void {
  localStorage.removeItem(TOKEN_KEY);
}

// ----------------------------------------------------------------------------
// 계약 에러(ApiErrorResponse)를 감싸는 커스텀 에러
// ----------------------------------------------------------------------------
// axios 인터셉터가 서버 에러 응답을 이 타입으로 정규화하여 호출자에게 던진다.
// 페이지 컴포넌트는 `err instanceof ApiError` 로 판별하고 `err.message`를 노출하면 된다.

export class ApiError extends Error {
  readonly code: string;
  readonly status: number;
  readonly details: ApiErrorResponse["details"];
  readonly path: string;
  readonly timestamp: string;

  constructor(payload: ApiErrorResponse) {
    super(payload.message);
    this.name = "ApiError";
    this.code = payload.code;
    this.status = payload.status;
    this.details = payload.details;
    this.path = payload.path;
    this.timestamp = payload.timestamp;
  }
}

/** 계약 shape을 만족하는 응답 본문인지 런타임 판별 */
function isApiErrorResponse(data: unknown): data is ApiErrorResponse {
  if (data === null || typeof data !== "object") return false;
  const d = data as Record<string, unknown>;
  return typeof d.code === "string" && typeof d.message === "string";
}

/**
 * 임의의 예외를 사용자 노출용 메시지로 변환한다.
 * 페이지에서 catch 후 메시지를 뽑을 때 사용.
 */
export function toErrorMessage(err: unknown): string {
  if (err instanceof ApiError) return err.message;
  if (err instanceof Error) return err.message;
  return "알 수 없는 오류가 발생했습니다.";
}

// ----------------------------------------------------------------------------
// axios 인스턴스
// ----------------------------------------------------------------------------

const baseURL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";

export const client: AxiosInstance = axios.create({
  baseURL,
  headers: { "Content-Type": "application/json" },
});

// 요청 인터셉터: 저장된 JWT가 있으면 모든 요청에 Bearer 헤더를 자동 첨부.
// permitAll 경로(상품 목록 등)에 토큰이 함께 가더라도 백엔드가 무시하므로 무방하다.
client.interceptors.request.use((config) => {
  const token = getToken();
  if (token) {
    config.headers.set("Authorization", `Bearer ${token}`);
  }
  return config;
});

// 응답 인터셉터:
//  - 401 → 저장 토큰 제거 후 로그인 페이지로 리다이렉트(로그인/회원가입 화면에서는 루프 방지).
//  - 그 외 → 계약 에러 스키마로 파싱하여 ApiError로 정규화해 던진다.
client.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ApiErrorResponse>) => {
    const status = error.response?.status;
    const data = error.response?.data;

    if (status === 401) {
      clearToken();
      const path = window.location.pathname;
      const onAuthPage = path.startsWith("/login") || path.startsWith("/signup");
      if (!onAuthPage) {
        window.location.assign("/login");
      }
    }

    if (isApiErrorResponse(data)) {
      return Promise.reject(new ApiError(data));
    }

    // 계약 shape이 아닌 경우(네트워크 단절, CORS, 5xx 비정형 응답 등)도
    // 동일한 ApiError 형태로 감싸 호출자가 일관되게 처리하도록 한다.
    return Promise.reject(
      new ApiError({
        code: status ? "HTTP_ERROR" : "NETWORK_ERROR",
        message: status
          ? `요청 처리 중 오류가 발생했습니다. (HTTP ${status})`
          : "서버에 연결할 수 없습니다. 네트워크 상태를 확인해 주세요.",
        details: null,
        timestamp: new Date().toISOString(),
        status: status ?? 0,
        path: error.config?.url ?? window.location.pathname,
      }),
    );
  },
);
