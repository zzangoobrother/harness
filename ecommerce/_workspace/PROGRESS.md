# 이커머스 MVP 빌드 진행 상황

> 내일 이어서 작업하기 위한 핸드오프 문서. 세션이 바뀌어도 이 문서 + `_workspace/contract/`만 보면 재개 가능하다.

**최종 갱신:** 2026-07-20 (Stage 0 완료 시점)

---

## 빌드 방식: 단계별(incremental)

사용자 요청에 따라 한 번에 만들지 않고 **도메인 단위로 한 단계씩** 진행한다.
각 단계 완료 후 멈추고, 사용자가 다음 단계를 지시하면 이어간다.

```
Stage 0: 계약(contract) 전체 설계          ✅ 완료
Stage 1: 인증 (회원가입/로그인 + JWT) + 프로젝트 스캐폴딩   ✅ 완료 (QA PASS)
Stage 2: 상품 (목록/상세)                  ⬜ 다음
Stage 3: 장바구니 (서버 저장)              ⬜
Stage 4: 주문/체크아웃 + 모의결제          ⬜
```

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

## 다음 단계(Stage 2: 상품) 재개 방법

1. 이 문서 + `_workspace/contract/` + `qa-report.md`를 읽어 컨텍스트 복원.
2. Stage 2 착수 시:
   - `backend-engineer` → 상품 도메인(`Product` 엔티티, `GET /api/products`=PageResponse, `GET /api/products/{id}`). 시드 데이터 고려.
   - `frontend-engineer` → 상품 목록/상세 화면, `api/products.ts`. 목록은 `PageResponse<Product>` 형태 주의.
   - 두 엔지니어는 계약을 먼저 읽고 shape을 정확히 일치시킨다. 계약 모순 발견 시 계약을 직접 고치지 말고 보고 → 오케스트레이터가 조율.
3. 완료 즉시 `qa-integrator`로 상품 모듈 경계면 검증. 결과는 `qa-report.md`에 누적.
4. 통과하면 멈추고 다음 단계 지시 대기.

---

## 미결/참고 항목

- 커밋은 로컬 `main` 기준으로 진행 중. 원격 푸시 여부는 사용자 결정.
- 저장소 루트 `.claude/settings.json`은 하네스와 별개 파일. 커밋 대상 여부 미정.
