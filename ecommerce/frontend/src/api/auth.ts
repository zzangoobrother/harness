// 인증 API 호출 함수. 경로/요청·응답 타입은 계약(api-spec.md, types.ts)과 100% 일치한다.

import { client } from "./client";
import type {
  AuthResponse,
  LoginRequest,
  SignupRequest,
} from "../types/contract";

/** POST /api/auth/signup — 회원가입 + JWT 발급 (성공 201, AuthResponse) */
export async function signup(body: SignupRequest): Promise<AuthResponse> {
  const res = await client.post<AuthResponse>("/api/auth/signup", body);
  return res.data;
}

/** POST /api/auth/login — 로그인 + JWT 발급 (성공 200, AuthResponse) */
export async function login(body: LoginRequest): Promise<AuthResponse> {
  const res = await client.post<AuthResponse>("/api/auth/login", body);
  return res.data;
}
