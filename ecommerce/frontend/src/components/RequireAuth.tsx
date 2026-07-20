// 보호 라우트 가드: 비로그인 상태에서 인증 필요 페이지에 접근하면 /login으로 리다이렉트한다.
// 프론트 가드는 UX용 방어선이며, 실제 보안 경계는 백엔드의 401 처리다(이중 방어).

import { Navigate, useLocation } from "react-router-dom";
import type { ReactNode } from "react";
import { useAuth } from "../store/AuthContext";

export function RequireAuth({ children }: { children: ReactNode }) {
  const { isAuthenticated } = useAuth();
  const location = useLocation();

  if (!isAuthenticated) {
    // 로그인 후 원래 가려던 위치로 되돌아올 수 있도록 state.from에 현재 위치를 담는다.
    return <Navigate to="/login" replace state={{ from: location }} />;
  }

  return <>{children}</>;
}
