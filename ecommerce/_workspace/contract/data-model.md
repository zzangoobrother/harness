# 데이터 모델 (data-model.md)

이커머스 MVP의 엔티티, 필드, 연관관계, 제약조건 정의. 이 문서는 `api-spec.md`, `types.ts`와
필드명·타입이 100% 일치해야 하며, backend-engineer(Spring Boot+JPA+PostgreSQL)의
엔티티 매핑과 frontend-engineer의 타입 소비 기준이 되는 단일 진실 공급원이다.

## 공통 규약

- **식별자 `id`**: 모든 엔티티는 PK로 `id`(DB `BIGINT`, JPA `Long`, 자동 증가)를 가진다. TS에서는 `number`.
- **필드명**: camelCase. DB 컬럼은 snake_case로 매핑되더라도 API 응답 JSON은 camelCase를 유지한다.
- **날짜/시간**: DB `TIMESTAMP`(UTC 저장 권장), API 응답은 ISO 8601 문자열(예: `2026-07-20T10:00:00Z`).
- **금액**: `INTEGER`(단위: 원, KRW). 소수·문자열로 섞지 않는다.
- **enum**: DB에는 문자열(`VARCHAR`)로 저장하며 값은 아래 정의된 철자를 그대로 사용한다.

## 엔티티 개요

| 엔티티 | 핵심 필드 | 관계 |
|--------|-----------|------|
| User | id, email(unique), passwordHash, name, role, createdAt | Cart 1:1, Order 1:N |
| Product | id, name, description, price, imageUrl, stock, category, createdAt | CartItem/OrderItem에서 N:1 참조 |
| Cart | id, userId(unique), createdAt | User 1:1, CartItem 1:N |
| CartItem | id, cartId, productId, quantity | Cart N:1, Product N:1 |
| Order | id, userId, totalAmount, status, createdAt | User N:1, OrderItem 1:N, Payment 1:1 |
| OrderItem | id, orderId, productId, quantity, priceAtOrder | Order N:1, Product N:1 |
| Payment | id, orderId(unique), amount, status, method, paidAt | Order 1:1 |

## 연관관계 다이어그램 (텍스트)

```
User  1 ------- 1  Cart
User  1 ------- N  Order
Cart  1 ------- N  CartItem
Order 1 ------- N  OrderItem
Order 1 ------- 1  Payment
Product 1 ----- N  CartItem
Product 1 ----- N  OrderItem
```

FK는 항상 N 쪽(또는 1:1의 종속 쪽)이 보유한다: Cart가 userId, CartItem이 cartId/productId,
Order가 userId, OrderItem이 orderId/productId, Payment가 orderId를 참조한다.

---

## 1. User

| 필드 | 타입 | nullable | 기본값 | unique | 설명 |
|------|------|----------|--------|--------|------|
| id | Long | N | auto | Y(PK) | 식별자 |
| email | String | N | - | Y | 로그인 이메일 |
| passwordHash | String | N | - | N | BCrypt 해시. **API 응답에 절대 미포함** |
| name | String | N | - | N | 사용자 이름 |
| role | UserRole(enum) | N | `USER` | N | `USER` \| `ADMIN` |
| createdAt | Timestamp | N | now | N | 생성 시각 |

- 제약: `email`은 유니크. 형식 검증(이메일 형식) 실패 시 400.
- 회원가입 시 동일 email 존재하면 409(`EMAIL_DUPLICATED`).
- `passwordHash`는 응답 DTO(`UserResponse`)에서 제외된다.

## 2. Product

| 필드 | 타입 | nullable | 기본값 | unique | 설명 |
|------|------|----------|--------|--------|------|
| id | Long | N | auto | Y(PK) | 식별자 |
| name | String | N | - | N | 상품명 |
| description | String | Y | null | N | 상품 설명(긴 텍스트) |
| price | Integer | N | - | N | 판매가(원), >= 0 |
| imageUrl | String | Y | null | N | 대표 이미지 URL |
| stock | Integer | N | 0 | N | 재고 수량, >= 0 |
| category | String | Y | null | N | 카테고리명 |
| createdAt | Timestamp | N | now | N | 생성 시각 |

- 제약: `price >= 0`, `stock >= 0`. 재고는 0 미만으로 내려갈 수 없다.
- 재고 차감은 주문 생성(체크아웃) 또는 결제 성공 시점에 수행한다(구현 규칙은 backend 스킬 참조).
  본 계약의 기본 가정: **주문 생성(POST /api/orders) 시점에 재고를 차감**한다(가정 참조).

## 3. Cart

| 필드 | 타입 | nullable | 기본값 | unique | 설명 |
|------|------|----------|--------|--------|------|
| id | Long | N | auto | Y(PK) | 식별자 |
| userId | Long(FK→User) | N | - | Y | 소유 사용자. 사용자당 1개 |
| createdAt | Timestamp | N | now | N | 생성 시각 |

- 제약: `userId` 유니크(User 1:1 Cart). 사용자가 최초로 장바구니에 접근할 때 없으면 생성한다(지연 생성).

## 4. CartItem

| 필드 | 타입 | nullable | 기본값 | unique | 설명 |
|------|------|----------|--------|--------|------|
| id | Long | N | auto | Y(PK) | 식별자 |
| cartId | Long(FK→Cart) | N | - | N | 소속 장바구니 |
| productId | Long(FK→Product) | N | - | N | 담긴 상품 |
| quantity | Integer | N | - | N | 수량, >= 1 |

- 제약: `(cartId, productId)` 복합 유니크 — 같은 장바구니에 동일 상품은 한 행으로만 존재.
  이미 담긴 상품을 다시 추가하면 수량을 합산한다.
- `quantity >= 1`. 0 이하로 변경 요청 시 400. 재고 초과 요청 시 409(`OUT_OF_STOCK`).

## 5. Order

| 필드 | 타입 | nullable | 기본값 | unique | 설명 |
|------|------|----------|--------|--------|------|
| id | Long | N | auto | Y(PK) | 식별자 |
| userId | Long(FK→User) | N | - | N | 주문한 사용자 |
| totalAmount | Integer | N | - | N | 주문 총액(원) = 모든 OrderItem subtotal 합 |
| status | OrderStatus(enum) | N | `PENDING` | N | `PENDING` \| `PAID` \| `FAILED` \| `CANCELLED` |
| createdAt | Timestamp | N | now | N | 주문 생성 시각 |

- 상태 전이: `PENDING` → (결제성공) `PAID` / (결제실패) `FAILED` / (취소) `CANCELLED`.
  MVP에서는 취소 API 미제공이므로 `CANCELLED`는 값만 예약한다(가정 참조).
- 주문 생성 시점 `status = PENDING`, `payment` 없음(null).

## 6. OrderItem

| 필드 | 타입 | nullable | 기본값 | unique | 설명 |
|------|------|----------|--------|--------|------|
| id | Long | N | auto | Y(PK) | 식별자 |
| orderId | Long(FK→Order) | N | - | N | 소속 주문 |
| productId | Long(FK→Product) | N | - | N | 주문한 상품 |
| quantity | Integer | N | - | N | 수량, >= 1 |
| priceAtOrder | Integer | N | - | N | **주문 시점 단가 스냅샷**(원) |

- `priceAtOrder`는 주문 생성 시점의 `Product.price`를 복사 저장한다. 이후 상품 가격이 바뀌어도
  과거 주문 금액이 왜곡되지 않도록 하기 위함이다.
- `subtotal`(= `priceAtOrder * quantity`)은 DB에 저장하지 않고 응답 계산으로 제공한다.

## 7. Payment

| 필드 | 타입 | nullable | 기본값 | unique | 설명 |
|------|------|----------|--------|--------|------|
| id | Long | N | auto | Y(PK) | 식별자 |
| orderId | Long(FK→Order) | N | - | Y | 대상 주문. 주문당 1건 |
| amount | Integer | N | - | N | 결제 금액(원) = Order.totalAmount |
| status | PaymentStatus(enum) | N | `PENDING` | N | `PENDING` \| `SUCCESS` \| `FAILED` |
| method | PaymentMethod(enum) | N | `MOCK` | N | `MOCK` (MVP는 모의결제만) |
| paidAt | Timestamp | Y | null | N | 결제 성공 시각. 성공 전이면 null |

- 제약: `orderId` 유니크(Order 1:1 Payment).
- 모의결제 실행 시 `simulateSuccess=true`이면 `status=SUCCESS`, `paidAt=now`, Order.status=`PAID`.
  `false`이면 `status=FAILED`, `paidAt=null`, Order.status=`FAILED`.
- 이미 `SUCCESS`인 주문에 재결제 요청 시 409(`ALREADY_PAID`).

---

## 가정 (설계 결정 근거)

- **가정 1 (금액 단위)**: 모든 금액은 정수 원(KRW). 소수점 통화를 쓰지 않으므로 Integer로 고정하여
  프론트/백엔드 간 반올림 불일치를 원천 차단한다.
- **가정 2 (재고 차감 시점)**: 재고는 **주문 생성(POST /api/orders) 시점**에 차감한다. 결제 실패 시
  복원 로직이 필요하지만, MVP 계약 관점에서는 "주문 생성 시 재고 부족이면 409"만 보장한다.
  (결제 실패 시 재고 복원 여부는 backend 구현 세부사항으로 위임; 계약상 GET 응답의 stock 값이 기준.)
- **가정 3 (장바구니 1:1)**: 사용자당 장바구니 1개, 없으면 최초 접근 시 지연 생성한다.
- **가정 4 (동일 상품 병합)**: 같은 장바구니에 동일 productId를 추가하면 새 행이 아니라 기존 행의
  quantity를 합산한다(`(cartId, productId)` 유니크).
- **가정 5 (인증 응답 통일)**: 회원가입과 로그인 모두 `AuthResponse { token, user }`를 반환한다.
  회원가입 직후 프론트가 재로그인 없이 인증 상태로 진입할 수 있게 하기 위함이다.
- **가정 6 (CANCELLED 예약)**: 주문 취소 API는 MVP 범위 밖이나, 상태 enum에 `CANCELLED`를 미리
  포함해 이후 프론트 분기 로직이 값 누락으로 깨지지 않도록 한다.
- **가정 7 (모의결제 플래그)**: 실 PG가 없으므로 `PaymentRequest.simulateSuccess`(생략 시 true)로
  성공/실패를 흉내 낸다. 결제 실패도 HTTP 200으로 응답하되 본문 `status=FAILED`로 구분한다
  (결제 "요청 처리"는 성공했고 결과가 실패이므로). 결제 불가 상태(이미 결제됨 등)만 4xx로 반환한다.

## 변경 이력

| 날짜 | 변경 내용 | 사유 |
|------|----------|------|
| 2026-07-20 | 초기 데이터 모델 확정 (User/Product/Cart/CartItem/Order/OrderItem/Payment) | Phase A / Stage 0 초기 설계 |
