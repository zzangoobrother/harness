// 회원가입 페이지 → POST /api/auth/signup
// 성공 시 AuthResponse{ token, user }로 즉시 인증 상태 확립(자동 로그인) 후 홈으로 이동.

import { useState, type FormEvent } from "react";
import { Link, useNavigate } from "react-router-dom";
import type { SignupRequest } from "../types/contract";
import { signup } from "../api/auth";
import { ApiError, toErrorMessage } from "../api/client";
import { useAuth } from "../store/AuthContext";

export function SignupPage() {
  const navigate = useNavigate();
  const { authenticate } = useAuth();

  const [form, setForm] = useState<SignupRequest>({
    email: "",
    password: "",
    name: "",
  });
  const [submitting, setSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  // 필드 단위 검증 오류(계약 ApiErrorResponse.details) 매핑: { field: reason }
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  const update =
    (key: keyof SignupRequest) => (e: React.ChangeEvent<HTMLInputElement>) => {
      setForm((prev) => ({ ...prev, [key]: e.target.value }));
    };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setSubmitting(true);
    setErrorMessage(null);
    setFieldErrors({});
    try {
      const auth = await signup(form);
      authenticate(auth);
      navigate("/", { replace: true });
    } catch (err) {
      if (err instanceof ApiError && err.details) {
        const map: Record<string, string> = {};
        for (const d of err.details) map[d.field] = d.reason;
        setFieldErrors(map);
      }
      setErrorMessage(toErrorMessage(err));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="auth-card">
      <h1 className="auth-title">회원가입</h1>

      {errorMessage && (
        <div className="alert alert-error" role="alert">
          {errorMessage}
        </div>
      )}

      <form className="form" onSubmit={handleSubmit} noValidate>
        <label className="field">
          <span className="field-label">이메일</span>
          <input
            type="email"
            value={form.email}
            onChange={update("email")}
            autoComplete="email"
            required
          />
          {fieldErrors.email && (
            <span className="field-error">{fieldErrors.email}</span>
          )}
        </label>

        <label className="field">
          <span className="field-label">이름</span>
          <input
            type="text"
            value={form.name}
            onChange={update("name")}
            autoComplete="name"
            required
          />
          {fieldErrors.name && (
            <span className="field-error">{fieldErrors.name}</span>
          )}
        </label>

        <label className="field">
          <span className="field-label">비밀번호</span>
          <input
            type="password"
            value={form.password}
            onChange={update("password")}
            autoComplete="new-password"
            required
          />
          {fieldErrors.password && (
            <span className="field-error">{fieldErrors.password}</span>
          )}
        </label>

        <button type="submit" className="btn btn-primary btn-block" disabled={submitting}>
          {submitting ? "가입 중..." : "회원가입"}
        </button>
      </form>

      <p className="auth-switch">
        이미 계정이 있으신가요? <Link to="/login">로그인</Link>
      </p>
    </div>
  );
}
