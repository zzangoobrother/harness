// 라우트 정의. 공통 Layout 아래에 각 페이지를 배치하고, 인증 필요 페이지는 RequireAuth로 감싼다.
// Stage 1: 인증(로그인/회원가입) + 보호된 홈. 상품/장바구니/주문/결제 라우트는 다음 단계에서 추가.

import { createBrowserRouter } from "react-router-dom";
import { Layout } from "./components/Layout";
import { RequireAuth } from "./components/RequireAuth";
import { HomePage } from "./pages/HomePage";
import { LoginPage } from "./pages/LoginPage";
import { SignupPage } from "./pages/SignupPage";

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
      // TODO(다음 단계): products, products/:id, cart, checkout, orders, orders/:id/complete
      { path: "*", element: <div className="home">페이지를 찾을 수 없습니다.</div> },
    ],
  },
]);
