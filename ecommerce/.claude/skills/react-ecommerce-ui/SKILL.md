---
name: react-ecommerce-ui
description: React + TypeScript(Vite)로 이커머스 프론트엔드(상품/장바구니/인증/주문/결제 화면)를 구현하는 방법론. frontend-engineer 에이전트가 사용한다. "프론트엔드 구현", "React 화면 작성", "UI 다시 구현", "장바구니 화면 수정", "체크아웃 페이지 보완", "라우팅 가드 추가" 요청 시 반드시 이 스킬을 사용하라. 반드시 `_workspace/contract/types.ts`와 `api-spec.md`를 먼저 읽고 그 타입·경로와 정확히 일치하는 API 호출 코드를 만든다.
---

# react-ecommerce-ui

React + TypeScript(Vite) 기반 이커머스 프론트엔드를 구현하는 절차다. 작업 시작 전 반드시
`_workspace/contract/types.ts`, `api-spec.md`를 읽고, API 클라이언트의 요청/응답 타입과
호출 경로를 계약과 100% 일치시켜라. 계약과 다른 필드명을 쓰면 컴파일은 통과해도(타입을
느슨하게 만든 경우) 런타임에 값이 `undefined`로 나오는, 발견하기 어려운 버그가 된다.

## Why: 타입 안정성과 계약 동기화의 이유

프론트가 백엔드 응답 shape을 추측해서 구현하면, 실제 서버가 다른 필드명을 내려줄 때 런타임까지
가서야 오류를 발견한다. `_workspace/contract/types.ts`를 그대로 가져와 API 응답 타입에 강제하면,
백엔드 구현이 계약과 다를 경우 TypeScript 컴파일 단계에서 즉시 타입 불일치가 드러난다(단, 이는
런타임 데이터가 실제로 그 타입을 만족한다고 "가정"하는 것이므로, 최종 검증은 여전히
integration-qa의 실제 응답 대조가 필요하다 — 타입은 개발 중 실수를 조기에 잡는 안전장치이지
런타임 보증이 아니다).

## 1. 프로젝트 구조

```
src/
├── api/
│   ├── client.ts        axios 인스턴스 + 인터셉터
│   ├── auth.ts          signup, login 호출 함수
│   ├── products.ts      상품 목록/상세 호출 함수
│   ├── cart.ts          장바구니 CRUD 호출 함수
│   └── orders.ts        주문/결제 호출 함수
├── types/
│   └── contract.ts       _workspace/contract/types.ts 내용을 반영(재수출 또는 복사 동기화)
├── store/ (또는 context/)
│   ├── AuthContext.tsx  로그인 상태, 토큰
│   └── CartContext.tsx  장바구니 상태
├── pages/
│   ├── ProductListPage.tsx
│   ├── ProductDetailPage.tsx
│   ├── CartPage.tsx
│   ├── LoginPage.tsx
│   ├── SignupPage.tsx
│   ├── CheckoutPage.tsx
│   ├── OrderCompletePage.tsx
│   └── OrderHistoryPage.tsx
├── components/            버튼, 카드, 헤더 등 재사용 UI
└── router.tsx              라우트 정의 + 가드
```

`src/types/contract.ts`는 계약의 `types.ts`를 그대로 옮기거나 재수출한다. 계약이 갱신되면
이 파일도 함께 갱신하여, 프론트 코드 전체가 항상 최신 계약 타입을 참조하게 한다.

## 2. API 클라이언트

- `src/api/client.ts`에 axios 인스턴스를 하나 만들고 `baseURL`을 환경변수로 관리한다
  (`import.meta.env.VITE_API_BASE_URL`).
- **요청 인터셉터**: 저장된 JWT가 있으면 모든 요청에 `Authorization: Bearer <token>` 헤더를
  자동으로 주입한다. 인증이 필요 없는 엔드포인트(GET 상품 목록 등)에도 토큰이 있으면 함께
  보내도 무방하다 — 백엔드가 permitAll 경로에서는 헤더를 무시하기 때문이다.
- **응답 인터셉터**: 401 응답을 받으면 저장된 토큰을 지우고 로그인 페이지로 리다이렉트한다.
  그 외 에러는 계약의 표준 에러 포맷(`ApiErrorResponse`)으로 파싱해 호출자에게 그대로 던진다.
- API 함수는 도메인별 파일(`auth.ts`, `products.ts`, `cart.ts`, `orders.ts`)로 나누고,
  각 함수의 파라미터/반환 타입은 `src/types/contract.ts`의 Request/Response 타입을 그대로 쓴다.

```typescript
// src/api/products.ts 예시
import { client } from "./client";
import type { Product } from "../types/contract";

export const getProducts = () => client.get<Product[]>("/api/products");
export const getProduct = (id: number) => client.get<Product>(`/api/products/${id}`);
```

## 3. 페이지 목록과 API 매핑

| 페이지 | 경로 | 호출 API |
|--------|------|----------|
| ProductListPage | `/` 또는 `/products` | GET /api/products |
| ProductDetailPage | `/products/:id` | GET /api/products/{id}, POST /api/cart/items |
| CartPage | `/cart` | GET /api/cart/items, PATCH /api/cart/items/{id}, DELETE /api/cart/items/{id} |
| LoginPage | `/login` | POST /api/auth/login |
| SignupPage | `/signup` | POST /api/auth/signup |
| CheckoutPage | `/checkout` | POST /api/orders |
| OrderCompletePage | `/orders/:id/complete` | GET /api/orders/{id}, POST /api/orders/{id}/payment |
| OrderHistoryPage | `/orders` | GET /api/orders |

각 페이지는 이 매핑을 벗어나지 않는다. 새 화면이 필요하면 먼저 계약에 대응 엔드포인트가
있는지 확인하고, 없으면 api-architect에게 계약 추가를 요청한 뒤 구현한다.

## 4. 상태관리

- **인증 상태**: `AuthContext`가 로그인 여부와 사용자 정보를 보관한다. JWT 자체는
  `localStorage`에 저장하고(새로고침에도 로그인 유지), Context는 메모리 상태(파싱된 사용자 정보,
  로그인 여부)만 들고 있는다. Why localStorage: 세션스토리지는 탭 종료 시 사라져 MVP의
  "로그인 유지" 기대와 맞지 않고, 쿠키 기반은 서버의 CORS/CSRF 설정이 추가로 필요해 MVP
  범위를 벗어난다. XSS 위험은 존재하므로 프로덕션 확장 시 httpOnly 쿠키 전환을 문서에 남겨라.
- **장바구니 상태**: `CartContext`가 서버 장바구니를 반영한 로컬 캐시를 들고 있는다. 장바구니는
  MVP 요구상 "서버 장바구니"이므로, 추가/수정/삭제 시 API를 호출한 뒤 응답으로 로컬 상태를
  갱신한다(낙관적 업데이트보다 서버 응답 기준 갱신을 우선하여 재고 초과 등 서버 검증 결과를
  놓치지 않는다).
- Context가 비대해지면(전역 상태가 늘어나면) Zustand 같은 경량 스토어로 전환할 수 있으나,
  MVP 범위에서는 Context + useReducer로 충분하다. 과설계하지 않는다.

## 5. 타입 안정성

- 계약의 `types.ts`를 `src/types/contract.ts`로 반영한다.
- API 함수의 반환 타입, 폼 상태 타입, Context 값 타입 모두 계약 타입을 참조한다. 새로 타입을
  정의하지 않는다 — 계약과 프론트 타입이 별도로 존재하면 둘이 어긋나는 순간 컴파일러가
  잡아주지 못한다.
- `tsc --noEmit`이 항상 통과하는 상태를 유지한다. `any`로 임시로 넘기지 않는다(부득이하면
  TODO 주석과 함께 남기고 계약 팀에 알린다).

## 6. 로딩/에러 UI와 라우팅 가드

- 모든 API 호출 지점에 최소한의 로딩 상태(스피너 또는 스켈레톤)와 에러 상태(계약의
  `ApiErrorResponse.message`를 사용자에게 노출)를 둔다. 빈 화면으로 방치하지 않는다.
- **라우팅 가드**: `/checkout`, `/orders`, `/orders/:id/complete` 등 인증이 필요한 페이지는
  `AuthContext`의 로그인 여부를 확인하는 가드 컴포넌트(`<RequireAuth>` 등)로 감싸, 비로그인
  상태에서 접근하면 `/login`으로 리다이렉트한다. 이 가드는 백엔드의 401 처리와 이중으로 방어선을
  이룬다 — 프론트 가드는 UX용, 백엔드 인증은 실제 보안 경계다. 둘 중 하나만 있으면 안 된다.

## 7. 계약 대조 체크리스트 (구현 완료 전 자체 점검)

- [ ] `src/types/contract.ts`가 `_workspace/contract/types.ts`와 동기화되어 있는가
- [ ] 모든 API 호출 경로/메서드가 `api-spec.md`와 일치하는가
- [ ] axios 인터셉터가 JWT를 `Authorization: Bearer` 형식으로 주입하는가
- [ ] 401 처리와 라우팅 가드가 실제로 동작하는가
- [ ] `tsc --noEmit`, `vite build`가 통과하는가

## 재호출 시 지침

- 재호출 시 기존 페이지/컴포넌트를 먼저 읽고 계약과의 차이 또는 요청된 변경분만 수정한다.
- 계약에 없는 필드/엔드포인트가 필요하면 임의로 만들지 말고 api-architect에게 계약 추가를
  요청한 뒤 계약이 갱신되면 구현한다.
- 모듈(인증/상품/장바구니/주문/결제) 하나가 완성될 때마다 qa-integrator가 즉시 검증할 수 있게
  해당 모듈의 페이지·API 호출을 완결된 단위로 만들어라.
