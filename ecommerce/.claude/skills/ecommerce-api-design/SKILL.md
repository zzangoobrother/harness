---
name: ecommerce-api-design
description: 이커머스 MVP의 데이터 모델과 REST API 계약을 설계·확정하는 방법론. api-architect 에이전트가 사용한다. "이커머스 계약 설계", "데이터 모델 정의", "API 스펙 작성", "엔드포인트 추가/수정", "계약 다시 설계", "타입 보완" 요청 시 반드시 이 스킬을 사용하라. backend-engineer·frontend-engineer가 병렬로 구현할 수 있는 single source of truth(_workspace/contract/)를 만드는 것이 목적이며, 이미 구현이 진행된 뒤 계약을 재조정할 때도 이 스킬로 기존 산출물을 갱신한다.
---

# ecommerce-api-design

이커머스 MVP(상품/장바구니/인증/주문/모의결제)의 데이터 모델과 REST API 계약을 설계하는 절차다.
결과물은 `_workspace/contract/` 아래 세 파일(`data-model.md`, `api-spec.md`, `types.ts`)이며,
이후 백엔드·프론트엔드 구현이 서로의 코드를 보지 않고도 병렬로 진행할 수 있게 하는 유일한 진실의 원천이다.

## Why: 계약을 먼저 고정하는 이유

백엔드와 프론트엔드가 각자 구현을 시작한 뒤 API 형태를 맞추면, 통합 시점에 필드명·경로·상태코드
충돌이 누적되어 되돌리기 비용이 커진다. 계약을 코드보다 먼저, 문서와 타입으로 고정하면
두 엔지니어가 서로를 기다리지 않고 동시에 작업할 수 있고, 통합 시점의 충돌은 "계약 위반 여부"라는
단순한 질문으로 축소된다. 그러므로 이 스킬의 산출물은 절대 모호해서는 안 된다 — 애매한 지점은
반드시 임의로 결정하고 근거를 문서에 남겨라. 결정을 미루면 그 모호함이 BE·FE 양쪽에서 각각
다르게 해석되어 재작업을 유발한다.

## 1. 데이터 모델 설계 절차 (ER 관점)

핵심 엔티티 다섯 개를 정의한다. 각 엔티티마다 아래 네 가지를 명시하라.

1. **필드 목록**: 이름(camelCase), 타입, nullable 여부, 기본값, unique 제약
2. **연관관계**: 상대 엔티티, 방향(1:N/N:1/N:N), FK를 갖는 쪽
3. **비즈니스 제약**: 예) 재고는 0 미만 불가, 이메일은 유니크
4. **상태값(있는 경우)**: enum 값 목록과 전이 규칙

권장 엔티티 구성 (MVP 범위 — 프로젝트 요구에 맞춰 필드를 가감하되 관계 방향은 유지):

| 엔티티 | 핵심 필드 | 관계 |
|--------|-----------|------|
| User | id, email(unique), passwordHash, name, role, createdAt | Cart 1:1, Order 1:N |
| Product | id, name, description, price, imageUrl, stock, category | CartItem/OrderItem에서 N:1 참조 |
| Cart | id, user(1:1) | CartItem 1:N |
| CartItem | id, cart(N:1), product(N:1), quantity | - |
| Order | id, user(N:1), totalAmount, status(enum), createdAt | OrderItem 1:N, Payment 1:1 |
| OrderItem | id, order(N:1), product(N:1), quantity, priceAtOrder | - |
| Payment | id, order(1:1), amount, status(enum), method(=MOCK), paidAt | - |

설계 원칙:
- **가격 스냅샷**: OrderItem에는 주문 시점 가격(`priceAtOrder`)을 별도로 저장한다. Product.price를
  나중에 참조하면 상품 가격이 바뀔 때 과거 주문 금액이 왜곡되기 때문이다.
- **상태 enum은 계약에 값 전부를 나열**한다. 예: Order.status = `PENDING | PAID | FAILED | CANCELLED`.
  값을 나중에 추가하면 프론트 분기 로직이 누락된 값을 처리하지 못한다.
- **FK는 항상 N 쪽이 보유**한다. Cart→CartItem, Order→OrderItem처럼 1:N 관계에서 N 쪽 엔티티가
  상위 엔티티의 id를 참조한다.
- 이 표는 출발점이다. 실제 요구사항(리뷰, 쿠폰 등)이 추가되면 동일한 절차로 엔티티를 확장하되,
  기존 엔티티의 필드명·타입은 하위호환을 깨지 않는 선에서만 바꾼다.

산출물 `data-model.md`에는 위 표 형태 + 각 엔티티의 제약조건 서술 + 연관관계 다이어그램(텍스트 화살표로 충분,
예: `User 1 --- 1 Cart`, `Cart 1 --- N CartItem`)을 포함하라.

## 2. REST 계약 규칙

### 리소스 네이밍
- 리소스는 복수형 명사, 소문자, 하이픈 구분: `/api/products`, `/api/cart/items`, `/api/orders`
- 중첩 리소스는 상위 리소스 경로 아래 둔다: `/api/orders/{orderId}/payment`
- 동사를 경로에 넣지 않는다(예: `/api/products/search`는 허용하되 `/api/getProducts`는 금지).

### HTTP 메서드 매핑
| 메서드 | 용도 | 성공 상태코드 |
|--------|------|---------------|
| GET | 조회, 부작용 없음 | 200 |
| POST | 생성, 또는 조회 불가능한 동작(로그인, 결제 실행) | 200/201 |
| PATCH | 부분 수정(예: 수량 변경) | 200 |
| PUT | 전체 치환(MVP에서는 사용 최소화) | 200 |
| DELETE | 삭제 | 200/204 |

### 요청/응답 DTO shape
- 요청 DTO와 응답 DTO는 별도 타입으로 분리한다. 엔티티를 그대로 직렬화하지 않는다
  (Why: 순환참조·비밀번호 해시 노출·오버페칭을 막기 위함이며, 이는 spring-boot-ecommerce 스킬의
  구현 규칙과 짝을 이룬다).
- 모든 필드명은 camelCase로 통일한다. 백엔드 언어가 Java라도 응답 JSON은 camelCase를 유지한다.
- 날짜/시간은 ISO 8601 문자열(`2026-07-19T10:00:00Z`)로 고정한다.
- 금액은 숫자 타입(정수 또는 소수, 프로젝트 내 단위를 문서에 명시)으로 통일하고 문자열로 섞지 않는다.
- 리스트 응답은 배열을 최상위로 반환할지, `{ items, totalCount }` 형태로 감쌀지 **하나로 통일**하고
  `api-spec.md`에 명시한다. 목록 API 전체가 같은 규칙을 따라야 한다.

### 표준 에러 응답 포맷
모든 실패 응답은 동일한 shape을 반환한다. 계약에 다음 스키마를 고정하라.

```json
{
  "timestamp": "2026-07-19T10:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "이메일 형식이 올바르지 않습니다.",
  "path": "/api/auth/signup"
}
```

이 포맷은 spring-boot-ecommerce의 `GlobalExceptionHandler`가 생성하고, react-ecommerce-ui의
axios 인터셉터가 소비하며, integration-qa가 대조 검증한다. 세 스킬이 이 하나의 shape을 공유한다는
점을 `api-spec.md` 상단에 명시하라.

### 상태 코드 규칙
200 조회 성공 · 201 생성 성공 · 400 검증 실패 · 401 미인증(토큰 없음/만료) · 403 권한 없음(본인 리소스
아님) · 404 리소스 없음 · 409 충돌(재고 부족, 이메일 중복) · 500 서버 오류.

### 페이지네이션
상품 목록처럼 데이터가 늘어나는 GET 엔드포인트는 쿼리 파라미터 `page`(0-base), `size`를 받고,
응답을 `{ items: T[], page, size, totalElements, totalPages }` 형태로 감싼다. 페이지네이션이
필요 없는 소규모 목록(장바구니 아이템 등)은 배열을 그대로 반환해도 되지만, 어떤 엔드포인트가
감싸는 형태인지 반드시 `api-spec.md`에 표시하라 — 이 구분이 불명확하면 integration-qa에서
가장 흔하게 걸리는 불일치가 된다.

## 3. MVP 엔드포인트 표

| 도메인 | 메서드 | 경로 | 인증 | 설명 |
|--------|--------|------|------|------|
| 인증 | POST | /api/auth/signup | 불필요 | 회원가입 |
| 인증 | POST | /api/auth/login | 불필요 | 로그인, JWT 발급 |
| 상품 | GET | /api/products | 불필요 | 상품 목록(페이지네이션) |
| 상품 | GET | /api/products/{id} | 불필요 | 상품 상세 |
| 장바구니 | GET | /api/cart/items | 필요 | 장바구니 조회 |
| 장바구니 | POST | /api/cart/items | 필요 | 아이템 추가 |
| 장바구니 | PATCH | /api/cart/items/{id} | 필요 | 수량 변경 |
| 장바구니 | DELETE | /api/cart/items/{id} | 필요 | 아이템 제거 |
| 주문 | POST | /api/orders | 필요 | 장바구니→주문 전환(체크아웃) |
| 주문 | GET | /api/orders | 필요 | 본인 주문 목록 |
| 주문 | GET | /api/orders/{id} | 필요 | 주문 상세 |
| 결제 | POST | /api/orders/{id}/payment | 필요 | 모의결제 실행 |

이 표는 최소 집합이다. 요구사항이 늘어나면 같은 패턴(리소스 명사 + 표준 메서드)으로 확장하되,
기존 엔드포인트의 경로/메서드는 하위호환이 필요하면 유지하고 새 버전이 필요하면 `api-spec.md`에
변경 이력을 남긴다.

## 4. 산출물 규칙

`_workspace/contract/`에 다음 세 파일을 생성한다. 세 파일은 서로 필드명·타입이 100% 일치해야 하며,
작업을 마치기 전 스스로 대조 검증하라.

1. **data-model.md** — 엔티티/필드/관계/제약조건 (1절 내용)
2. **api-spec.md** — 엔드포인트별 메서드·경로·요청/응답 JSON 예시·상태코드·에러케이스 (2, 3절 내용)
3. **types.ts** — 프론트가 그대로 import할 TypeScript 인터페이스. Product, User, Cart, CartItem,
   Order, OrderItem, Payment 및 각 엔드포인트의 Request/Response DTO를 전부 export한다.

```typescript
// types.ts 예시 발췌
export interface Product {
  id: number;
  name: string;
  description: string;
  price: number;
  imageUrl: string;
  stock: number;
  category: string;
}

export interface ApiErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
}
```

이 계약은 backend-engineer와 frontend-engineer의 **single source of truth**다. 두 엔지니어는
자신의 판단으로 필드명이나 경로를 바꾸지 않고, 변경이 필요하면 반드시 이 스킬(api-architect)에게
SendMessage로 요청해 계약을 먼저 갱신한 뒤 구현을 따라간다.

## 5. 완료 전 자체 검증 체크리스트

- [ ] MVP 엔드포인트 표의 모든 항목이 `api-spec.md`에 존재하는가
- [ ] `types.ts`의 모든 인터페이스 필드명이 `api-spec.md` 예시 JSON과 정확히 일치하는가
- [ ] 모든 엔드포인트가 표준 에러 포맷을 참조하고 있는가
- [ ] 인증 필요 여부가 모든 엔드포인트에 명시되어 있는가
- [ ] enum 값(주문/결제 status)이 세 파일 모두에서 동일한 철자로 나열되어 있는가
- [ ] 날짜/금액 필드의 타입과 직렬화 형식이 일관적인가

## 재설계/수정 시 지침

- 재호출 시 `_workspace/contract/` 기존 파일을 먼저 전부 읽고, 처음부터 다시 설계하지 않는다.
  요청된 변경분만 반영하고 나머지는 보존한다.
- 이미 구현이 진행된 상태에서 계약을 바꿀 경우, 변경 사유와 영향받는 엔드포인트/필드를
  각 파일 하단 "변경 이력" 섹션에 기록한다.
- 계약 변경 완료 후에는 backend-engineer, frontend-engineer 양쪽에 무엇이 바뀌었는지 통지한다
  (통지는 요약만, 계약 본문 전체를 메시지에 반복하지 않는다).
