// 홈(대시보드) 페이지 — 인증 필요.
// 상품 · 장바구니 · 주문 내역 각 도메인 화면으로 가는 진입점을 모아둔다.

import { Link } from "react-router-dom";
import { useAuth } from "../store/AuthContext";

export function HomePage() {
  const { user } = useAuth();

  return (
    <div className="home">
      <h1>환영합니다, {user?.name}님</h1>
      <p className="muted">로그인되었습니다. ({user?.email})</p>
      <Link to="/products" className="btn btn-primary">
        상품 보러가기
      </Link>
      <Link to="/cart" className="btn">
        장바구니
      </Link>
      <Link to="/orders" className="btn">
        주문 내역
      </Link>
    </div>
  );
}
