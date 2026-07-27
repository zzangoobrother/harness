# 이커머스 MVP 빌드 진행 상황

> 내일 이어서 작업하기 위한 핸드오프 문서. 세션이 바뀌어도 이 문서 + `_workspace/contract/`만 보면 재개 가능하다.

**최종 갱신:** 2026-07-27 (Stage 4 완료 — **MVP 전 단계 완료**)

---

## 빌드 방식: 단계별(incremental)

사용자 요청에 따라 한 번에 만들지 않고 **도메인 단위로 한 단계씩** 진행한다.
각 단계 완료 후 멈추고, 사용자가 다음 단계를 지시하면 이어간다.

```
Stage 0: 계약(contract) 전체 설계          ✅ 완료
Stage 1: 인증 (회원가입/로그인 + JWT) + 프로젝트 스캐폴딩   ✅ 완료 (QA PASS)
Stage 2: 상품 (목록/상세)                  ✅ 완료 (QA PASS)
Stage 3: 장바구니 (서버 저장)              ✅ 완료 (QA PASS)
Stage 4: 주문/체크아웃 + 모의결제          ✅ 완료 (QA PASS) — 마지막 단계
```

> **MVP 완료.** 계약의 12개 엔드포인트(인증 2 · 상품 2 · 장바구니 4 · 주문 3 · 결제 1)가 전부 구현·검증되었다.

- **계약은 통짜로 Stage 0에서 확정**했고, **구현만 단계별**로 나눈다 (계약을 도메인별로 쪼개면 필드 충돌이 오히려 늘기 때문).
- 프로젝트 스캐폴딩(Spring Boot / Vite 뼈대 생성)은 Stage 1 착수 시 해당 엔지니어가 자기 디렉터리를 잡으며 수행한다.

---

## 오케스트레이션 주의사항 (중요)

하네스 에이전트 정의는 `ecommerce/.claude/agents/`에 있으나, **세션의 프로젝트 루트가 상위인 `harness/`**라서
`subagent_type: "api-architect"` 식의 자동 등록이 **안 된다** (`harness/.claude/agents`는 비어 있음).

**우회 방식(현재 채택):** `general-purpose` 서브에이전트를 띄우되, 프롬프트에서
해당 역할 정의 파일(`.claude/agents/*.md`)과 스킬(`.claude/skills/*/SKILL.md`)을 **직접 Read 하여 역할을 주입**한다.
- 스킬 자체는 `ecommerce/` 스코프 스킬로 세션에 자동 등록되어 있어 서브에이전트가 Skill 툴로도 쓸 수 있다.

> 근본 해결(선택): 에이전트/스킬을 repo 루트 `.claude/`로 올리거나, `ecommerce/`를 독립 프로젝트 루트로 열면 자동 등록된다. 지금은 진행을 막지 않는 우회 방식으로 간다.

---

## Stage 0 산출물 — 계약 (single source of truth)

`_workspace/contract/` 에 3파일 생성 완료. BE·FE 구현의 유일한 기준이다.

| 파일 | 내용 |
|------|------|
| `data-model.md` | 엔티티 7개, 필드/관계/제약조건 |
| `api-spec.md` | 엔드포인트 12개, 요청·응답 JSON 예시, 에러케이스 |
| `types.ts` | 프론트가 그대로 import할 TS 타입/DTO |

**엔티티(7):** User, Product, Cart, CartItem, Order, OrderItem, Payment
**엔드포인트(12):** 인증 2 · 상품 2 · 장바구니 4 · 주문 3 · 결제 1

**핵심 계약 결정 (구현 시 반드시 준수):**
- **금액은 정수 원(KRW)** — float/문자열 금지. 반올림 불일치 차단.
- **가격 스냅샷** — `OrderItem.priceAtOrder`에 주문 당시 가격 보존.
- **재고 차감 시점** = 주문 생성(`POST /api/orders`).
- **장바구니** = 사용자당 1개 지연 생성, 동일 productId는 quantity 합산. 조회/조작 응답은 모두 `CartResponse` 전체 반환으로 통일.
- **모의결제** — `simulateSuccess` 플래그(생략 시 true)로 성공/실패 흉내.
  - 결제 실패는 **HTTP 200 + 본문 `status=FAILED`** (요청 처리는 성공했으므로).
  - 이미 결제된 주문 재결제만 **409 (ALREADY_PAID)**.
- **공통 에러 스키마:** `{ code, message, details, timestamp, status, path }`.
  - 주요 code: VALIDATION_ERROR, INVALID_CREDENTIALS, UNAUTHORIZED, FORBIDDEN, NOT_FOUND, EMAIL_DUPLICATED, OUT_OF_STOCK, CART_EMPTY, ALREADY_PAID, INTERNAL_ERROR
- **인증** — signup/login 모두 `AuthResponse{ token, user }` 반환(가입 직후 자동 인증). 인증 필요 엔드포인트는 `Authorization: Bearer <token>`.
- **목록 형태** — 상품=`PageResponse`, 주문=배열, 장바구니=`CartResponse`.
- 필드명 camelCase, 날짜 ISO 8601 string.

**enum 값 (세 파일 일치 확인됨):**
- OrderStatus: `PENDING | PAID | FAILED | CANCELLED` (CANCELLED는 MVP 취소 API 없으나 예약)
- PaymentStatus: `PENDING | SUCCESS | FAILED`
- PaymentMethod: `MOCK`

---

## Stage 1 산출물 요약 (완료, QA PASS)

- **backend/** — Spring Boot **4.0.x + Jackson 3 + JDK 25 + Gradle wrapper**. Security/JWT 필터, `GlobalExceptionHandler`(공통 에러 스키마), `User` 엔티티, `POST /api/auth/signup`(201)·`/login`(200). `./gradlew test` 6/6 통과. dev=PostgreSQL, test=H2.
- **frontend/** — Vite+React19+TS. `api/client.ts`(Bearer 자동첨부/401 리다이렉트/에러 정규화), `types/contract.ts`(계약 복사본, 전 도메인 타입 포함), `AuthContext`+`RequireAuth`, 회원가입/로그인 페이지. `tsc`·`vite build` 통과.
- **경계면 검증:** `_workspace/qa-report.md`에 "인증 모듈 검증 (1차)" — 전 항목 일치, 불일치 0건, PASS.

**재사용 인프라 위치 (다음 도메인이 그대로 씀):**
- BE: `common/exception/ErrorCode`에 상품/장바구니/주문/결제 코드 **이미 전부 등록**. 컨트롤러에서 `@AuthenticationPrincipal AuthPrincipal principal`로 userId 취득. `GET /api/products/**`는 permitAll 세팅됨.
- FE: `src/api/client.ts` import해서 `products.ts`/`cart.ts`/`orders.ts` 추가. `router.tsx`에 다음 도메인 라우트 TODO 자리표시. 보호 화면은 `<RequireAuth>`로 감싼다.
- **환경 주의:** Spring Boot 4 + Jackson 3 (ObjectMapper=`tools.jackson.databind.ObjectMapper`, MockMvc 슬라이스=`org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc`, `write-dates-as-timestamps` 속성 제거됨).

## Stage 2 산출물 요약 (완료, QA PASS)

- **backend/** — `product/` 패키지(entity/repository/service/controller/dto/mapper) + `common/dto/PageResponse`.
  `GET /api/products`(page/size/category, 기본 0/20) · `GET /api/products/{id}`. 둘 다 permitAll.
  `ProductSeeder`(`@Profile("!test")`)가 dev에 상품 12개(electronics/fashion/home) 시드. `cleanTest test` **16/16 통과**.
- **frontend/** — `api/products.ts`(`listProducts`/`getProduct`), `ProductListPage`(그리드·카테고리 필터·페이지네이션·품절 배지),
  `ProductDetailPage`(404 분기, 장바구니 버튼은 Stage 3 자리표시자 disabled). `products`·`products/:id` 라우트는
  **RequireAuth 미적용**(계약상 인증 불필요). `npm run build` 통과.
- **경계면 검증:** `qa-report.md` "상품 모듈 검증 (2차)" — 불일치 2건 발견→해결, 최종 미해결 0건, PASS.

**Stage 2에서 고친 공통 인프라 (Stage 3/4가 그대로 수혜):**
- `GlobalExceptionHandler`에 `MethodArgumentTypeMismatchException` · `MissingServletRequestParameterException`
  핸들러 추가 → 잘못된 타입의 `@RequestParam`/`@PathVariable`이 **500이 아니라 400 VALIDATION_ERROR**로 응답.
  (수정 전에는 `/api/products/abc`가 500이었고, 이 결함은 `/api/cart/items/{id}`·`/api/orders/{id}`에도 전파될 것이었음.)
- `common/dto/PageResponse<T>` 생성. 이후 목록 페이지네이션이 필요하면 `PageResponse.from(Page)`를 재사용한다.
  **Spring Data `Page<T>`를 직접 직렬화하면 계약 위반**(`content/pageable/...`)이므로 반드시 이 DTO를 거칠 것.

**검증 시 반드시 지킬 것:** `./gradlew test`만 실행하면 `UP-TO-DATE`로 **테스트를 건너뛴다**.
반드시 `./gradlew cleanTest test`로 강제 재실행해야 실제 통과를 확인할 수 있다.

## Stage 3 산출물 요약 (완료, QA PASS)

- **backend/** — `cart/` 패키지(entity/repository/service/controller/dto/mapper).
  `GET /api/cart`(200, 지연 생성) · `POST /api/cart/items`(**201**) · `PATCH /api/cart/items/{id}`(200) ·
  `DELETE /api/cart/items/{id}`(200). 4개 모두 인증 필요이며 **응답은 항상 `CartResponse` 전체**.
  `Cart.userId` unique, `CartItem`에 `(cart_id, product_id)` **복합 유니크**. 재고는 **검증만** 하고 차감하지 않는다.
  `cleanTest test` **31/31 통과**(cart 15 + auth 6 + product 10, 회귀 0).
- **frontend/** — `api/cart.ts`(getCart/addCartItem/updateCartItem/removeCartItem), `CartPage`(목록·수량변경·삭제·
  빈 카트·아이템별 에러), `/cart` 라우트(**RequireAuth 적용**), `Layout`에 장바구니 링크(인증 시 노출),
  `ProductDetailPage`의 담기 버튼 활성화 + 수량 스테퍼. `tsc --noEmit`·`npm run build` 통과.
- **경계면 검증:** `qa-report.md` "장바구니 모듈 검증 (3차)" — 불일치 **0건**, PASS.
  구현 에이전트 자기보고를 신뢰하지 않고 오케스트레이터가 테스트 XML·빌드를 직접 재현했다.

**Stage 3에서 추가된 재사용 자산 (Stage 4가 그대로 씀):**
- `common/exception/ForbiddenException`(403 FORBIDDEN) — **주문 도메인 소유권 검증에 그대로 재사용**.
- `common/exception/OutOfStockException`(409 OUT_OF_STOCK) — 주문 생성 시 재고 차감 검증에 재사용.
- `CartService.getOrCreateCart(userId)` / `CartItemRepository.findByCartId(cartId)` —
  체크아웃이 "현재 장바구니 전체"를 읽을 때 그대로 호출하면 된다.
- FE: `CartPage`의 에러코드 분기 패턴(`err instanceof ApiError && err.code === ...`)을 체크아웃 화면에 재사용.

**Stage 3에서 남긴 주의점 (Stage 4에 직접 영향):**
- `CartService.insertNewItem`의 경합 복구(`DataIntegrityViolationException` catch 후 같은 트랜잭션에서 병합)는
  JPA가 트랜잭션을 rollback-only로 표시하므로 **의도대로 동작하지 않을 가능성이 높다**.
  DB 유니크 제약이라는 1차 방어선은 정상이라 데이터는 오염되지 않는다.
  **Stage 4에서 재고 차감에 같은 패턴(예외 잡고 같은 트랜잭션에서 복구)을 쓰지 말 것** — 낙관적 락 또는
  `UPDATE ... WHERE stock >= ?`의 갱신 행 수 확인 방식이 안전하다.
- `buildCartResponse`는 아이템의 상품이 사라지면 404를 던진다. 상품 삭제 API가 없어 현재는 도달 불가.

## Stage 4 산출물 요약 (완료, QA PASS)

- **backend/** — `order/` 패키지(entity/repository/service/controller/dto/mapper) + `payment/` 패키지 +
  공통 예외 3개(`OrderNotFoundException` 404 · `CartEmptyException` 409 · `AlreadyPaidException` 409, **새 ErrorCode 추가 없음**).
  `POST /api/orders`(**201**, 본문 `{}` 또는 생략) · `GET /api/orders`(**배열**, 최신순) ·
  `GET /api/orders/{id}`(200) · `POST /api/orders/{id}/payment`(**200**). 4개 모두 인증 필요.
  `cleanTest test` **50/50 통과**(order 19 신규 + auth 6 + product 10 + cart 15, 회귀 0).
- **frontend/** — `api/orders.ts`(createOrder/listOrders/getOrder/payOrder), `CheckoutPage`(장바구니 요약·총액·
  CART_EMPTY/OUT_OF_STOCK 분기), `OrderHistoryPage`(배열 응답·최신순·상태 배지), `OrderCompletePage`(주문 상세 +
  모의결제 실행·결과), `OrderStatusBadge`, `utils/format.ts`. `checkout`·`orders`·`orders/:id/complete` 라우트
  **전부 `<RequireAuth>`**. `Layout`에 주문 내역 링크, `CartPage`에 체크아웃 진입점. `tsc --noEmit`·`npm run build` 통과.
- **경계면 검증:** `qa-report.md` "주문/결제 모듈 검증 (4차)" — 불일치 **0건**, PASS.
  오케스트레이터가 테스트 XML·양쪽 빌드·핵심 코드 경로를 직접 재현 확인했다.

**Stage 4의 핵심 구현 결정 (이후 확장 시 반드시 인지할 것):**
- **재고 차감 동시성** — `ProductRepository.decreaseStockIfAvailable`의 조건부 UPDATE
  (`UPDATE Product SET stock = stock - :qty WHERE id = :id AND stock >= :qty`) 후 **갱신 행 수가 0이면 OUT_OF_STOCK**.
  Stage 3이 경고한 "예외 catch 후 같은 트랜잭션에서 복구" 패턴을 쓰지 않았다.
  `clearAutomatically`는 **의도적으로 미사용** — 같은 트랜잭션에서 Cart/CartItem을 삭제해야 하는데
  컨텍스트를 비우면 준영속이 되어 깨진다.
- **결제 실패 시 재고를 복원하지 않는다.** 계약상 FAILED 주문은 재결제가 가능하므로, 실패 시 되돌리면
  재결제 성공 시 재고 차감 없이 판매가 확정된다. 주문 취소 API가 생기면 **그 시점에** 복원해야 한다.
- **재결제 정책** — `409 ALREADY_PAID`는 **SUCCESS 주문에만**. FAILED 주문은 BE·FE 모두 재시도를 허용하며,
  Payment는 주문당 1건(orderId 유니크)이라 재시도 시 같은 행을 갱신한다(`payment.id` 동일).
- **`simulateSuccess` 기본값 계약** — BE가 래퍼 `Boolean`으로 받고 `null → true`. FE가 인자를 생략하면
  JSON 직렬화가 키를 제거해 `{}`가 전송되므로, **primitive `boolean`으로 바꾸면 "생략 = 결제 실패"로 동작이 뒤집힌다.**
  회귀 방지 테스트 2개(`pay_withEmptyJsonObject_defaultsToSuccess`, `pay_withNoBodyAtAll_defaultsToSuccess`)로 고정해 두었다.

---

## 알려진 계약 이슈 (미해결, 차단 사유 아님)

- **계약 내부 모순 — OrderItem의 상품명 스냅샷 부재.**
  `data-model.md` §6의 OrderItem 필드는 `orderId/productId/quantity/priceAtOrder`뿐인데
  `types.ts`의 `OrderItemResponse`는 `productName`을 요구한다. 구현은 data-model을 따라 이름을 저장하지 않고
  응답 조립 시 Product를 조인해 채웠다. 결과적으로 **가격은 스냅샷이지만 상품명은 스냅샷이 아니다.**
  또한 상품이 삭제되면 과거 주문 조회가 404가 된다(`OrderService.requireProduct`).
  상품 삭제/수정 API가 없어 MVP에서는 도달 불가. **상품 삭제 기능을 추가하려면 `OrderItem.productName` 컬럼을
  먼저 도입하고 계약 두 파일을 정렬해야 한다.**
- `PaymentService`에 `CANCELLED` 주문 결제 가드가 없다(FE만 막음). 취소 API 부재로 `CANCELLED` 생성 경로가
  없어 도달 불가. **취소 API 도입 시 BE 가드 추가 필수.**
- Stage 3의 `CartService.insertNewItem` 경합 복구 패턴은 여전히 의도대로 동작하지 않을 가능성이 높다
  (DB 유니크 제약이 1차 방어선이라 데이터는 오염되지 않음). 정리하려면 별도 작업이 필요하다.

---

## 미결/참고 항목

- 커밋은 로컬 `main` 기준으로 진행 중. 원격 푸시 여부는 사용자 결정.
- 저장소 루트 `.claude/settings.json`은 하네스와 별개 파일. 커밋 대상 여부 미정.
- **실제 구동 검증 미수행** — dev 프로파일이 로컬 PostgreSQL을 요구해 BE·FE를 실제로 띄운 end-to-end
  확인은 하지 않았다. 검증은 통합 테스트(MockMvc가 실제 직렬화 JSON을 파싱)와 빌드까지다.
