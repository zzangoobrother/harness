// 앱 진입점: 인증 컨텍스트로 전체를 감싸고 라우터를 제공한다.

import { RouterProvider } from "react-router-dom";
import { AuthProvider } from "./store/AuthContext";
import { router } from "./router";

export default function App() {
  return (
    <AuthProvider>
      <RouterProvider router={router} />
    </AuthProvider>
  );
}
