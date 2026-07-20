# REST API 계약 (api-spec.md)

이커머스 MVP의 엔드포인트별 메서드·경로·인증·요청/응답 JSON·상태코드·에러케이스 정의.
`data-model.md`, `types.ts`와 필드명·타입이 100% 일치한다. backend-engineer와
frontend-engineer가 서로의 코드를 보지 않고 병렬 구현할 수 있는 단일 진실 공급원이다.

## 공통 규약

- **Base URL**: 모든 경로는 `/api` 접두. (예: `http://localhost:8080/api/products`)
- **필드명**: camelCase. **금액**: 정수(원). **날짜/시간**: ISO 8601 문자열.
- **인증**: 인증 필요 엔드포인트는 요청에 `Authorization: Bearer <token>` 헤더 필수.
  토큰은 `POST /api/auth/login` 또는 `POST /api/auth/signup` 응답의 `token` 필드.
- **Content-Type**: 요청/응답 본문은 `application/json`.

### 표준 에러 응답 (모든 실패 응답 공통 shape)

모든 4xx/5xx 응답은 아래 단일 스키마(`ApiErrorResponse`)를 반환한다. 이 shape은
백엔드 `GlobalExceptionHandler`가 생성하고 프론트 axios 인터셉터가 소비하며 qa-integrator가 대조한다.

```json
{
  "code": "VALIDATION_ERROR",
  "message": "이메일 형식이 올바르지 않습니다.",
  "details": [
    { "field": "email", "reason": "올바른 이메일 형식이 아닙니다." }
  ],
  "timestamp": "2026-07-20T10:00:00Z",
  "status": 400,
  "path": "/api/auth/signup"
}
```

- `details`는 필드 단위 검증 오류가 없으면 `null`.
- 주요 `code` 값(관례): `VALIDATION_ERROR`(400), `UNAUTHORIZED`(401), `FORBIDDEN`(403),
  `NOT_FOUND`(404), `EMAIL_DUPLICATED`(409), `OUT_OF_STOCK`(409), `ALREADY_PAID`(409),
  `INVALID_CREDENTIALS`(401), `CART_EMPTY`(409), `INTERNAL_ERROR`(500).

### 상태 코드 규칙

| 코드 | 의미 |
|------|------|
| 200 | 조회/처리 성공 |
| 201 | 리소스 생성 성공 |
| 400 | 요청 검증 실패 |
| 401 | 미인증(토큰 없음/만료/자격증명 오류) |
| 403 | 권한 없음(타인 리소스 접근) |
| 404 | 리소스 없음 |
| 409 | 충돌(이메일 중복, 재고 부족, 이미 결제됨, 빈 장바구니) |
| 500 | 서버 오류 |

### 목록 응답 형태 규칙

- **상품 목록**(`GET /api/products`): 데이터 증가 대상이므로 `PageResponse<Product>`로 감싼다.
- **장바구니**(`GET /api/cart`): 소규모이므로 `CartResponse`(items 배열 내포)로 반환.
- **주문 목록**(`GET /api/orders`): MVP 소규모이므로 **배열**(`OrderResponse[]`)을 그대로 반환(페이지네이션 없음).

---

## 엔드포인트 요약표

| 도메인 | 메서드 | 경로 | 인증 | 성공코드 | 설명 |
|--------|--------|------|------|----------|------|
| 인증 | POST | /api/auth/signup | 불필요 | 201 | 회원가입 + JWT 발급 |
| 인증 | POST | /api/auth/login | 불필요 | 200 | 로그인, JWT 발급 |
| 상품 | GET | /api/products | 불필요 | 200 | 상품 목록(페이지네이션) |
| 상품 | GET | /api/products/{id} | 불필요 | 200 | 상품 상세 |
| 장바구니 | GET | /api/cart | 필요 | 200 | 내 장바구니 조회 |
| 장바구니 | POST | /api/cart/items | 필요 | 201 | 아이템 추가 |
| 장바구니 | PATCH | /api/cart/items/{id} | 필요 | 200 | 수량 변경 |
| 장바구니 | DELETE | /api/cart/items/{id} | 필요 | 200 | 아이템 제거 |
| 주문 | POST | /api/orders | 필요 | 201 | 체크아웃(장바구니→주문) |
| 주문 | GET | /api/orders | 필요 | 200 | 내 주문 목록 |
| 주문 | GET | /api/orders/{id} | 필요 | 200 | 주문 상세 |
| 결제 | POST | /api/orders/{id}/payment | 필요 | 200 | 모의결제 실행 |

---

## 1. 인증

### 1.1 POST /api/auth/signup — 회원가입
- 인증: **불필요**
- 요청 본문 (`SignupRequest`):
```json
{ "email": "user@example.com", "password": "secret123", "name": "홍길동" }
```
- 성공 201 (`AuthResponse`):
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": 1,
    "email": "user@example.com",
    "name": "홍길동",
    "role": "USER",
    "createdAt": "2026-07-20T10:00:00Z"
  }
}
```
- 에러: 400(`VALIDATION_ERROR` — 이메일 형식/필수값 누락), 409(`EMAIL_DUPLICATED` — 이메일 중복).

### 1.2 POST /api/auth/login — 로그인
- 인증: **불필요**
- 요청 본문 (`LoginRequest`):
```json
{ "email": "user@example.com", "password": "secret123" }
```
- 성공 200 (`AuthResponse`): (구조는 1.1의 성공 응답과 동일)
- 에러: 400(`VALIDATION_ERROR`), 401(`INVALID_CREDENTIALS` — 이메일/비밀번호 불일치).

---

## 2. 상품

### 2.1 GET /api/products — 상품 목록
- 인증: **불필요**
- 쿼리 파라미터: `page`(0-base, 기본 0), `size`(기본 20), `category`(선택, 카테고리 필터).
  예: `GET /api/products?page=0&size=20&category=electronics`
- 성공 200 (`PageResponse<Product>`):
```json
{
  "items": [
    {
      "id": 1,
      "name": "무선 이어폰",
      "description": "노이즈 캔슬링 지원",
      "price": 129000,
      "imageUrl": "https://cdn.example.com/img/1.jpg",
      "stock": 50,
      "category": "electronics",
      "createdAt": "2026-07-20T09:00:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```
- 에러: 400(`VALIDATION_ERROR` — 잘못된 page/size).

### 2.2 GET /api/products/{id} — 상품 상세
- 인증: **불필요**
- 성공 200 (`Product`):
```json
{
  "id": 1,
  "name": "무선 이어폰",
  "description": "노이즈 캔슬링 지원",
  "price": 129000,
  "imageUrl": "https://cdn.example.com/img/1.jpg",
  "stock": 50,
  "category": "electronics",
  "createdAt": "2026-07-20T09:00:00Z"
}
```
- 에러: 404(`NOT_FOUND` — 존재하지 않는 상품).

---

## 3. 장바구니 (모두 인증 필요)

응답은 항상 장바구니 전체(`CartResponse`)를 반환하여 프론트가 추가 조회 없이 상태를 갱신하도록 한다.

### 3.1 GET /api/cart — 내 장바구니 조회
- 인증: **필요** (`Authorization: Bearer <token>`)
- 성공 200 (`CartResponse`):
```json
{
  "id": 10,
  "items": [
    {
      "id": 100,
      "productId": 1,
      "productName": "무선 이어폰",
      "productImageUrl": "https://cdn.example.com/img/1.jpg",
      "price": 129000,
      "quantity": 2,
      "subtotal": 258000
    }
  ],
  "totalAmount": 258000
}
```
- 에러: 401(`UNAUTHORIZED`). 장바구니가 없으면 서버가 지연 생성 후 빈 카트(`items: []`, `totalAmount: 0`) 반환.

### 3.2 POST /api/cart/items — 아이템 추가
- 인증: **필요**
- 요청 본문 (`AddCartItemRequest`):
```json
{ "productId": 1, "quantity": 2 }
```
- 성공 201 (`CartResponse`): (3.1과 동일 구조, 갱신된 전체 장바구니)
- 동작: 이미 담긴 productId면 수량 합산. `quantity >= 1`.
- 에러: 400(`VALIDATION_ERROR` — quantity < 1 등), 401(`UNAUTHORIZED`),
  404(`NOT_FOUND` — 상품 없음), 409(`OUT_OF_STOCK` — 재고 부족).

### 3.3 PATCH /api/cart/items/{id} — 수량 변경
- 인증: **필요**
- 경로 파라미터: `{id}` = CartItem id.
- 요청 본문 (`UpdateCartItemRequest`):
```json
{ "quantity": 3 }
```
- 성공 200 (`CartResponse`): 갱신된 전체 장바구니.
- 에러: 400(`VALIDATION_ERROR` — quantity < 1), 401(`UNAUTHORIZED`),
  403(`FORBIDDEN` — 타인 장바구니 아이템), 404(`NOT_FOUND` — 아이템 없음),
  409(`OUT_OF_STOCK` — 재고 초과).

### 3.4 DELETE /api/cart/items/{id} — 아이템 제거
- 인증: **필요**
- 경로 파라미터: `{id}` = CartItem id.
- 요청 본문: 없음.
- 성공 200 (`CartResponse`): 해당 아이템 제거 후 전체 장바구니.
- 에러: 401(`UNAUTHORIZED`), 403(`FORBIDDEN`), 404(`NOT_FOUND`).

---

## 4. 주문 (모두 인증 필요)

### 4.1 POST /api/orders — 체크아웃(장바구니→주문 전환)
- 인증: **필요**
- 요청 본문 (`CheckoutRequest`): 빈 객체 `{}` (현재 서버 장바구니 전체를 주문으로 전환).
```json
{}
```
- 동작: 현재 사용자 장바구니의 모든 아이템으로 Order + OrderItem 생성.
  각 OrderItem.priceAtOrder = 그 시점 Product.price. totalAmount = 합계.
  주문 생성 시 재고 차감(가정 2). status=`PENDING`, payment=`null`. 성공 시 장바구니는 비운다.
- 성공 201 (`OrderResponse`):
```json
{
  "id": 500,
  "status": "PENDING",
  "totalAmount": 258000,
  "items": [
    {
      "id": 600,
      "productId": 1,
      "productName": "무선 이어폰",
      "quantity": 2,
      "priceAtOrder": 129000,
      "subtotal": 258000
    }
  ],
  "payment": null,
  "createdAt": "2026-07-20T10:05:00Z"
}
```
- 에러: 401(`UNAUTHORIZED`), 409(`CART_EMPTY` — 빈 장바구니), 409(`OUT_OF_STOCK` — 재고 부족).

### 4.2 GET /api/orders — 내 주문 목록
- 인증: **필요**
- 성공 200 (`OrderResponse[]` — **배열**, 최신순 권장):
```json
[
  {
    "id": 500,
    "status": "PAID",
    "totalAmount": 258000,
    "items": [
      {
        "id": 600,
        "productId": 1,
        "productName": "무선 이어폰",
        "quantity": 2,
        "priceAtOrder": 129000,
        "subtotal": 258000
      }
    ],
    "payment": {
      "id": 700,
      "orderId": 500,
      "amount": 258000,
      "status": "SUCCESS",
      "method": "MOCK",
      "paidAt": "2026-07-20T10:06:00Z"
    },
    "createdAt": "2026-07-20T10:05:00Z"
  }
]
```
- 에러: 401(`UNAUTHORIZED`).

### 4.3 GET /api/orders/{id} — 주문 상세
- 인증: **필요**
- 경로 파라미터: `{id}` = Order id.
- 성공 200 (`OrderResponse`): (4.2 배열 요소와 동일 구조)
- 에러: 401(`UNAUTHORIZED`), 403(`FORBIDDEN` — 타인 주문), 404(`NOT_FOUND`).

---

## 5. 모의결제 (인증 필요)

### 5.1 POST /api/orders/{id}/payment — 모의결제 실행
- 인증: **필요**
- 경로 파라미터: `{id}` = Order id.
- 요청 본문 (`PaymentRequest`): `simulateSuccess` 생략 시 true.
```json
{ "simulateSuccess": true }
```
- 동작:
  - `simulateSuccess=true` → Payment.status=`SUCCESS`, paidAt=now, Order.status=`PAID`.
  - `simulateSuccess=false` → Payment.status=`FAILED`, paidAt=null, Order.status=`FAILED`.
  - 결제 실패도 처리는 정상 수행되었으므로 **HTTP 200**으로 응답하고 본문 `status`로 결과를 구분한다(가정 7).
- 성공 200 (`PaymentResponse`) — 성공 예:
```json
{
  "id": 700,
  "orderId": 500,
  "amount": 258000,
  "status": "SUCCESS",
  "method": "MOCK",
  "paidAt": "2026-07-20T10:06:00Z"
}
```
- 실패 시뮬레이션 예 (여전히 HTTP 200):
```json
{
  "id": 700,
  "orderId": 500,
  "amount": 258000,
  "status": "FAILED",
  "method": "MOCK",
  "paidAt": null
}
```
- 에러: 401(`UNAUTHORIZED`), 403(`FORBIDDEN` — 타인 주문), 404(`NOT_FOUND` — 주문 없음),
  409(`ALREADY_PAID` — 이미 SUCCESS인 주문 재결제).

---

## 가정 (설계 결정 근거)

- **가정 A (에러 스키마 통합)**: 과업에서 요구한 핵심 3필드 `{ code, message, details }`를 기본으로 하되,
  디버깅/로깅에 유용한 `timestamp`, `status`, `path`를 부가 필드로 함께 제공한다(`ApiErrorResponse`).
  프론트는 `code`/`message`/`details`만 소비해도 충분하다.
- **가정 B (장바구니 조회 경로)**: 과업 지정에 따라 조회는 `GET /api/cart`, 아이템 조작은 `/api/cart/items`
  하위 경로를 사용한다.
- **가정 C (장바구니 응답 통일)**: 추가/수정/삭제 모든 응답을 `CartResponse` 전체로 반환하여
  프론트가 추가 GET 없이 상태를 갱신한다.
- **가정 D (주문 목록 비페이지네이션)**: MVP 주문 수가 적으므로 `GET /api/orders`는 배열을 그대로 반환한다.
  향후 증가 시 `PageResponse<OrderResponse>`로 전환하고 변경 이력에 남긴다.
- **가정 E (모의결제 실패 = 200)**: 결제 실패는 요청 처리 성공/결과 실패이므로 200 + 본문 status로 구분,
  결제 불가 상태(이미 결제됨 등)만 409로 반환한다.

## 변경 이력

| 날짜 | 변경 내용 | 사유 |
|------|----------|------|
| 2026-07-20 | 초기 API 계약 확정 (인증/상품/장바구니/주문/모의결제 12개 엔드포인트) | Phase A / Stage 0 초기 설계 |
