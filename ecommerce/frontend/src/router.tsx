// 라우트 정의. 공통 Layout 아래에 각 페이지를 배치하고, 인증 필요 페이지는 RequireAuth로 감싼다.
// Stage 1: 인증(로그인/회원가입) + 보호된 홈.
// Stage 2: 상품 목록/상세 추가 — 계약상 인증 불필요(permitAll)이므로 RequireAuth로 감싸지 않는다.
// Stage 3: 장바구니 추가 — 계약상 인증 필요이므로 RequireAuth로 감싼다.
// Stage 4: 체크아웃/주문 목록/주문 상세+결제 추가 — 계약상 모두 인증 필요이므로 RequireAuth로 감싼다.

import { createBrowserRouter } from "react-router-dom";
import { Layout } from "./components/Layout";
import { RequireAuth } from "./components/RequireAuth";
import { HomePage } from "./pages/HomePage";
import { LoginPage } from "./pages/LoginPage";
import { SignupPage } from "./pages/SignupPage";
import { ProductListPage } from "./pages/ProductListPage";
import { ProductDetailPage } from "./pages/ProductDetailPage";
import { CartPage } from "./pages/CartPage";
import { CheckoutPage } from "./pages/CheckoutPage";
import { OrderHistoryPage } from "./pages/OrderHistoryPage";
import { OrderCompletePage } from "./pages/OrderCompletePage";

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
      {
        path: "cart",
        element: (
          <RequireAuth>
            <CartPage />
          </RequireAuth>
        ),
      },
      {
        path: "checkout",
        element: (
          <RequireAuth>
            <CheckoutPage />
          </RequireAuth>
        ),
      },
      {
        path: "orders",
        element: (
          <RequireAuth>
            <OrderHistoryPage />
          </RequireAuth>
        ),
      },
      {
        path: "orders/:id/complete",
        element: (
          <RequireAuth>
            <OrderCompletePage />
          </RequireAuth>
        ),
      },
      { path: "*", element: <div className="home">페이지를 찾을 수 없습니다.</div> },
    ],
  },
]);
