// 로그인 페이지 → POST /api/auth/login
// 성공 시 AuthResponse{ token, user }로 인증 상태 확립 후,
// 가드에 의해 밀려났던 원래 목적지(state.from)가 있으면 그곳으로, 없으면 홈으로 이동.

import { useState, type FormEvent } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import type { LoginRequest } from "../types/contract";
import { login } from "../api/auth";
import { ApiError, toErrorMessage } from "../api/client";
import { useAuth } from "../store/AuthContext";

interface LocationState {
  from?: { pathname?: string };
}

export function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { authenticate } = useAuth();

  const redirectTo =
    (location.state as LocationState | null)?.from?.pathname ?? "/";

  const [form, setForm] = useState<LoginRequest>({ email: "", password: "" });
  const [submitting, setSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  const update =
    (key: keyof LoginRequest) => (e: React.ChangeEvent<HTMLInputElement>) => {
      setForm((prev) => ({ ...prev, [key]: e.target.value }));
    };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setSubmitting(true);
    setErrorMessage(null);
    setFieldErrors({});
    try {
      const auth = await login(form);
      authenticate(auth);
      navigate(redirectTo, { replace: true });
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
      <h1 className="auth-title">로그인</h1>

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
          <span className="field-label">비밀번호</span>
          <input
            type="password"
            value={form.password}
            onChange={update("password")}
            autoComplete="current-password"
            required
          />
          {fieldErrors.password && (
            <span className="field-error">{fieldErrors.password}</span>
          )}
        </label>

        <button type="submit" className="btn btn-primary btn-block" disabled={submitting}>
          {submitting ? "로그인 중..." : "로그인"}
        </button>
      </form>

      <p className="auth-switch">
        아직 계정이 없으신가요? <Link to="/signup">회원가입</Link>
      </p>
    </div>
  );
}
