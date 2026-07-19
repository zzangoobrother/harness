---
name: spring-boot-ecommerce
description: Spring Boot + JPA + PostgreSQL로 이커머스 백엔드(인증/상품/장바구니/주문/모의결제)를 구현하는 방법론. backend-engineer 에이전트가 사용한다. "백엔드 구현", "Spring Boot 코드 작성", "API 구현", "백엔드 다시 구현", "주문 로직 수정", "결제 로직 보완", "재고 차감 추가" 요청 시 반드시 이 스킬을 사용하라. 반드시 `_workspace/contract/`의 계약(data-model.md, api-spec.md, types.ts)을 먼저 읽고 그 응답 shape과 정확히 일치하는 구현을 만든다.
---

# spring-boot-ecommerce

Spring Boot + JPA + PostgreSQL 기반 이커머스 백엔드를 구현하는 절차다. 작업 시작 전 반드시
`_workspace/contract/data-model.md`, `api-spec.md`, `types.ts`를 읽고, 엔티티 필드·엔드포인트·
에러 포맷을 계약과 100% 일치시켜라. 계약과 다르게 구현하면 아무리 코드 품질이 좋아도
integration-qa 단계에서 실패로 판정된다.

## Why: 계층을 분리하고 계약을 따르는 이유

계층(Controller/Service/Repository/Entity/DTO)을 분리하지 않으면 비즈니스 로직이 컨트롤러에
섞여 트랜잭션 경계가 불명확해지고, 테스트가 어려워지며, 엔티티가 API 응답으로 그대로 나가면서
비밀번호 해시 같은 민감 필드가 노출되거나 순환참조로 직렬화가 깨진다. 계약을 따르는 이유는
frontend-engineer가 동시에 같은 계약을 보고 구현하고 있기 때문이다 — 백엔드가 계약과 다른
shape을 내보내면 프론트는 그 사실을 모른 채 타입만 맞춰 구현하다가 통합 시점에야 발견하게 된다.

## 1. 프로젝트 구조: 기능별 패키지 × 계층

패키지를 계층 우선(controller/, service/ 전체를 한데 묶기)이 아니라 **기능(도메인) 우선**으로
나눈다. 기능마다 필요한 계층 하위 패키지를 둔다.

```
src/main/java/.../ecommerce/
├── auth/
│   ├── controller/  AuthController
│   ├── service/     AuthService
│   ├── dto/         SignupRequest, LoginRequest, LoginResponse
│   └── entity/       (User는 auth 또는 user 패키지에 둔다)
├── product/
│   ├── controller/  ProductController
│   ├── service/     ProductService
│   ├── repository/  ProductRepository
│   ├── entity/      Product
│   └── dto/         ProductResponse
├── cart/
│   ├── controller/  CartController
│   ├── service/     CartService
│   ├── repository/  CartRepository, CartItemRepository
│   ├── entity/      Cart, CartItem
│   └── dto/         CartItemRequest, CartResponse
├── order/
│   ├── controller/  OrderController
│   ├── service/     OrderService
│   ├── repository/  OrderRepository, OrderItemRepository
│   ├── entity/      Order, OrderItem
│   └── dto/         OrderRequest, OrderResponse
├── payment/
│   ├── controller/  PaymentController
│   ├── service/     PaymentService
│   ├── entity/      Payment
│   └── dto/         PaymentResponse
├── common/
│   ├── exception/   커스텀 예외, GlobalExceptionHandler
│   └── dto/         ApiErrorResponse
└── config/
    └── security/    SecurityConfig, JwtAuthFilter, JwtTokenProvider
```

기능 우선 구조를 쓰는 이유: 한 기능(예: 주문)을 수정할 때 열어야 할 파일이 한 폴더에 모여 있어
탐색 비용이 줄고, 여러 엔지니어가 서로 다른 기능을 동시에 작업할 때 파일 충돌이 최소화된다.

## 2. 계층별 책임과 규칙

| 계층 | 책임 | 규칙 |
|------|------|------|
| Controller | HTTP 요청/응답 매핑만 | 얇게 유지. 비즈니스 로직 금지. `@Valid`로 요청 DTO 검증, Service 호출, 응답 DTO 반환. |
| Service | 비즈니스 로직, 트랜잭션 경계 | 상태를 바꾸는 메서드에 `@Transactional`. 여러 Repository 호출을 조합. 도메인 규칙(재고 검증, 상태 전이) 위치. |
| Repository | 데이터 접근 | Spring Data JPA `JpaRepository` 상속. 커스텀 쿼리는 메서드 이름 규칙 또는 `@Query`로. |
| Entity | 영속 모델, 연관관계 매핑 | 연관관계는 기본 `FetchType.LAZY`(N:1도 명시적으로 LAZY 지정 권장). setter 남용 대신 의미 있는 메서드(`decreaseStock`, `changeStatus`) 제공. |
| DTO | 요청/응답 전송 객체 | 요청 DTO와 응답 DTO를 분리. 엔티티를 컨트롤러 밖으로 직접 노출하지 않는다. |
| Mapper | Entity ↔ DTO 변환 | 정적 메서드 또는 별도 Mapper 클래스. Service 또는 별도 mapper 패키지에 위치. |
| Exception/Config | 횡단 관심사 | GlobalExceptionHandler, SecurityConfig 등 기능 패키지 밖 `common/`, `config/`에 위치. |

**엔티티를 API로 직접 노출하지 않는 이유(Why):** 엔티티에는 연관관계(양방향 참조)가 있어 그대로
직렬화하면 무한 순환참조로 예외가 나거나, Jackson 설정으로 억지로 끊어도 필요 없는 연관 엔티티까지
통째로 내려가는 오버페칭이 발생한다. 또한 `passwordHash` 같은 필드가 실수로 응답에 섞여 나갈
위험이 있다. DTO를 분리하면 "계약에 정의된 필드만" 정확히 내려보낼 수 있다.

## 3. JWT 보안

- `SecurityConfig`: `/api/auth/**`, `/api/products/**`(GET)는 permitAll, 나머지는 인증 필요.
  세션을 쓰지 않으므로 `SessionCreationPolicy.STATELESS`로 설정한다.
- `JwtAuthFilter`(`OncePerRequestFilter`): `Authorization: Bearer <token>` 헤더를 읽어 토큰을
  검증하고, 유효하면 `SecurityContext`에 인증 정보를 채운다. 토큰이 없거나 만료되면 필터를
  통과시키되 인증 객체를 세팅하지 않아 이후 인가 단계에서 401이 나가도록 한다.
- `JwtTokenProvider`: 로그인 성공 시 토큰 발급(사용자 id/email을 클레임에 포함), 서명 검증,
  만료 검증 메서드를 제공한다.
- 비밀번호는 `BCryptPasswordEncoder`로 해싱하여 저장한다. 평문 비교를 절대 하지 않는다.
- 인증 필요/불필요 엔드포인트는 계약(`api-spec.md`)의 인증 컬럼을 그대로 따른다. 임의로
  범위를 넓히거나 좁히지 않는다.

## 4. 예외 처리

- 도메인별 커스텀 예외를 정의한다(예: `ProductNotFoundException`, `InsufficientStockException`,
  `DuplicateEmailException`, `InvalidCredentialsException`). 각 예외는 대응하는 HTTP 상태코드를
  내부적으로 가지거나 핸들러에서 매핑한다.
- `@RestControllerAdvice`로 `GlobalExceptionHandler`를 만들어 모든 예외를 계약의 표준 에러
  포맷으로 변환한다.

```java
// 계약의 표준 에러 포맷을 그대로 반영한 응답 예시
{
  "timestamp": "2026-07-19T10:00:00Z",
  "status": 409,
  "error": "Conflict",
  "message": "재고가 부족합니다.",
  "path": "/api/orders"
}
```

- `MethodArgumentNotValidException`(Bean Validation 실패), 커스텀 도메인 예외, 그리고 그 외
  모든 예외를 잡는 fallback 핸들러까지 세 단계로 구성하여 어떤 예외도 표준 포맷을 벗어나지
  않게 한다. Why: 프론트가 모든 에러를 동일한 shape으로 파싱하기 때문에, 한 곳이라도 다른
  shape을 내보내면 그 화면에서만 에러 처리가 깨진다.

## 5. 검증

- 요청 DTO 필드에 Bean Validation 애노테이션을 붙인다: `@NotNull`, `@NotBlank`, `@Email`,
  `@Positive`(가격/수량), `@Size`(문자열 길이) 등.
- Controller 파라미터에 `@Valid`를 명시해야 검증이 실제로 동작한다.
- 검증 실패는 400 + 표준 에러 포맷으로 응답한다(4절의 GlobalExceptionHandler가 처리).

## 6. 모의결제(PaymentService)

실제 PG 연동 없이 결제를 시뮬레이션한다. 결정론적이고 테스트 가능한 규칙을 사용하라(예:
주문 금액이 특정 임계값을 넘으면 실패 처리, 또는 항상 성공하되 실패 케이스를 별도 트리거로
테스트 가능하게 만드는 방식 중 하나를 프로젝트 요구에 맞춰 선택하고 `data-model.md`/`api-spec.md`의
가정과 일치시킨다).

처리 흐름:
1. `POST /api/orders/{id}/payment` 요청을 받으면 해당 주문이 본인 소유이고 `PENDING` 상태인지 확인한다.
2. 모의 결제 규칙을 적용해 성공/실패를 결정한다.
3. `Payment` 엔티티를 생성하고 `status`를 `SUCCESS`/`FAILED`로 기록한다.
4. **같은 트랜잭션 안에서** `Order.status`를 `PAID`(성공) 또는 `FAILED`(실패)로 전이시킨다.
5. 실패 시 재고를 원복할지 여부를 결정하고 문서화한다(권장: 주문 생성 시점에 이미 재고를 차감했다면
   결제 실패 시 재고를 복구한다 — 재고가 묶여 있는 채로 방치되지 않도록).

Order 상태 전이는 반드시 유효한 흐름만 허용한다: `PENDING → PAID | FAILED`, `PAID`/`FAILED`에서는
재결제를 막는다. 잘못된 상태에서 결제를 시도하면 409 Conflict로 응답한다.

## 7. 재고 차감과 장바구니→주문 전환

`OrderService`의 체크아웃 로직(`POST /api/orders`)은 아래 순서를 하나의 `@Transactional` 안에서 수행한다.

1. 현재 사용자의 Cart를 조회하고 비어 있으면 400/409로 거부한다.
2. CartItem마다 Product의 재고를 확인한다. 부족하면 `InsufficientStockException`(409)을 던지고
   **어떤 상품의 재고도 차감하지 않는다** — 부분 실패를 허용하지 않는다(all-or-nothing).
3. 검증을 통과하면 각 CartItem을 OrderItem으로 변환하며 `priceAtOrder`에 현재 Product.price를
   스냅샷으로 저장한다.
4. Product.stock을 차감한다.
5. Order를 `PENDING` 상태로 생성하고 totalAmount를 계산한다.
6. Cart를 비운다(CartItem 삭제).

Why 트랜잭션 하나로 묶는 이유: 재고 차감과 주문 생성이 분리된 트랜잭션이면 중간에 실패했을 때
재고만 줄고 주문은 안 생기는 정합성 붕괴가 발생한다.

## 8. 계약 대조 체크리스트 (구현 완료 전 자체 점검)

- [ ] 모든 응답 DTO의 필드명이 `types.ts`의 대응 인터페이스와 정확히 일치하는가(대소문자 포함)
- [ ] 모든 엔드포인트 경로/메서드가 `api-spec.md`와 일치하는가
- [ ] 에러 응답이 전부 표준 포맷(`timestamp, status, error, message, path`)을 따르는가
- [ ] 날짜 필드가 ISO 8601 문자열로 직렬화되는가
- [ ] 인증 필요 엔드포인트에 `JwtAuthFilter`가 실제로 적용되는가
- [ ] 프로젝트 빌드(`./gradlew build` 또는 `mvn test`)가 통과하는가

## 재호출 시 지침

- 재호출 시 기존 구현을 먼저 읽고 계약과의 차이만 수정한다. 통째로 다시 작성하지 않는다.
- 계약과 요구사항이 충돌하면 임의로 구현을 바꾸지 말고 api-architect에게 계약 수정을 요청한 뒤
  계약이 갱신되면 구현을 따라간다.
- 모듈(인증/상품/장바구니/주문/결제) 하나가 완성될 때마다 qa-integrator가 즉시 검증할 수 있도록
  해당 모듈만 완결된 상태로 커밋 가능한 단위로 만들어라.
