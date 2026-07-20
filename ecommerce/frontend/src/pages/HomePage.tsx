// 홈(임시 대시보드) 페이지 — 인증 필요.
// Stage 1에서는 인증 흐름 검증용 자리표시자다. 상품 목록 등은 다음 단계에서 이 자리에 구현한다.

import { useAuth } from "../store/AuthContext";

export function HomePage() {
  const { user } = useAuth();

  return (
    <div className="home">
      <h1>환영합니다, {user?.name}님</h1>
      <p className="muted">로그인되었습니다. ({user?.email})</p>
      <p className="muted">
        상품 목록 · 장바구니 · 주문/결제 화면은 다음 단계에서 이 영역에 추가됩니다.
      </p>
    </div>
  );
}
