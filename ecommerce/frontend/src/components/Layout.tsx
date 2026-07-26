// 공통 레이아웃: 상단 헤더(로고 + 인증 상태별 내비게이션) + 본문 아웃렛.
// 이후 상품/장바구니/주문 화면도 이 레이아웃을 공유한다.

import { Link, Outlet, useNavigate } from "react-router-dom";
import { useAuth } from "../store/AuthContext";

export function Layout() {
  const { user, isAuthenticated, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate("/login", { replace: true });
  };

  return (
    <div className="app-shell">
      <header className="app-header">
        <Link to="/" className="brand">
          이커머스
        </Link>
        <nav className="app-nav">
          <Link to="/products" className="btn btn-ghost">
            상품
          </Link>
          {isAuthenticated ? (
            <>
              <Link to="/cart" className="btn btn-ghost">
                장바구니
              </Link>
              <span className="nav-user">{user?.name}님</span>
              <button type="button" className="btn btn-ghost" onClick={handleLogout}>
                로그아웃
              </button>
            </>
          ) : (
            <>
              <Link to="/login" className="btn btn-ghost">
                로그인
              </Link>
              <Link to="/signup" className="btn btn-primary">
                회원가입
              </Link>
            </>
          )}
        </nav>
      </header>
      <main className="app-main">
        <Outlet />
      </main>
    </div>
  );
}
