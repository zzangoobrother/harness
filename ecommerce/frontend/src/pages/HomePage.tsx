// 홈(임시 대시보드) 페이지 — 인증 필요.
// Stage 1에서는 인증 흐름 검증용 자리표시자였고, Stage 2부터 상품 목록으로 이동하는 진입점을 추가한다.
// 장바구니 · 주문/결제 화면은 다음 단계에서 이 영역에 추가된다.

import { Link } from "react-router-dom";
import { useAuth } from "../store/AuthContext";

export function HomePage() {
  const { user } = useAuth();

  return (
    <div className="home">
      <h1>환영합니다, {user?.name}님</h1>
      <p className="muted">로그인되었습니다. ({user?.email})</p>
      <p className="muted">장바구니 · 주문/결제 화면은 다음 단계에서 이 영역에 추가됩니다.</p>
      <Link to="/products" className="btn btn-primary">
        상품 보러가기
      </Link>
    </div>
  );
}
