// 라우트 정의. 공통 Layout 아래에 각 페이지를 배치하고, 인증 필요 페이지는 RequireAuth로 감싼다.
// Stage 1: 인증(로그인/회원가입) + 보호된 홈.
// Stage 2: 상품 목록/상세 추가 — 계약상 인증 불필요(permitAll)이므로 RequireAuth로 감싸지 않는다.
// 장바구니/주문/결제 라우트는 다음 단계에서 추가.

import { createBrowserRouter } from "react-router-dom";
import { Layout } from "./components/Layout";
import { RequireAuth } from "./components/RequireAuth";
import { HomePage } from "./pages/HomePage";
import { LoginPage } from "./pages/LoginPage";
import { SignupPage } from "./pages/SignupPage";
import { ProductListPage } from "./pages/ProductListPage";
import { ProductDetailPage } from "./pages/ProductDetailPage";

export const router = createBrowserRouter([
  {
    path: "/",
    element: <Layout />,
    children: [
      {
        index: true,
        element: (
          <RequireAuth>
            <HomePage />
          </RequireAuth>
        ),
      },
      { path: "login", element: <LoginPage /> },
      { path: "signup", element: <SignupPage /> },
      { path: "products", element: <ProductListPage /> },
      { path: "products/:id", element: <ProductDetailPage /> },
      // TODO(다음 단계): cart, checkout, orders, orders/:id/complete
      { path: "*", element: <div className="home">페이지를 찾을 수 없습니다.</div> },
    ],
  },
]);
