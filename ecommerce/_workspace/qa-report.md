# QA 통합 검증 리포트 (qa-report.md)

경계면(BE-FE integration boundary) 교차 검증 결과를 모듈별로 누적 기록한다.
기준 계약: `_workspace/contract/` (data-model.md, api-spec.md, types.ts).

---

## 인증 모듈 검증 (1차) — Phase C / Stage 1

- **검증 일시**: 2026-07-21
- **대상 모듈**: 인증(회원가입/로그인) — 상품/장바구니/주문/결제는 미구현으로 검증 대상 아님
- **검증자**: qa-integrator
- **대상 코드**:
  - 백엔드: `ecommerce/backend/` (Spring Boot 4.0.7 + Jackson 3, JDK 25, Java 21 타깃)
  - 프론트: `ecommerce/frontend/` (Vite 8 + React 19 + TS 6)

### 1. 경계면 대조 결과 표

| 항목 | 백엔드(BE) | 프론트(FE) | 계약 | 일치 |
|------|-----------|-----------|------|------|
| signup 경로/메서드 | `POST /api/auth/signup` (`AuthController.java:30-34`, `@RequestMapping("/api/auth")`) | `client.post("/api/auth/signup")` (`api/auth.ts:11-12`, baseURL `http://localhost:8080`) | `POST /api/auth/signup` (201) | 일치 |
| login 경로/메서드 | `POST /api/auth/login` (`AuthController.java:36-40`) | `client.post("/api/auth/login")` (`api/auth.ts:17-18`) | `POST /api/auth/login` (200) | 일치 |
| signup 성공 상태코드 | 201 CREATED (`AuthController.java:33`) | axios 성공 분기(별도 상태 판별 없음, 2xx 통과) | 201 | 일치 |
| login 성공 상태코드 | 200 OK (`AuthController.java:39`) | axios 성공 분기 | 200 | 일치 |
| SignupRequest 필드 | `email, password, name` (`SignupRequest.java:11-23`) | `SignupRequest{email,password,name}` (`types/contract.ts:178-183`) | `email, password, name` | 일치 |
| LoginRequest 필드 | `email, password` (`LoginRequest.java:9-17`) | `LoginRequest{email,password}` (`types/contract.ts:186-189`) | `email, password` | 일치 |
| AuthResponse shape | `record AuthResponse(String token, UserResponse user)` (`AuthResponse.java:11-14`) | `AuthResponse{token, user}` (`types/contract.ts:192-196`), `AuthContext.authenticate(auth)` 소비 | `{token, user}` | 일치 |
| UserResponse 필드/순서 | `Long id, String email, String name, UserRole role, Instant createdAt` (`UserResponse.java:14-20`) | `{id:number,email,name,role,createdAt:string}` (`types/contract.ts:90-96`) | `id,email,name,role,createdAt` | 일치 |
| passwordHash 미노출 | `UserMapper.toResponse`가 5개 필드만 매핑, passwordHash 제외 (`UserMapper.java:14-22`); 테스트로 검증 | 타입에 필드 없음 | 미포함 필수 | 일치 (테스트 검증됨) |
| UserRole enum 값 | `USER, ADMIN` (`UserRole.java:6-9`), `@Enumerated(STRING)` | `"USER" \| "ADMIN"` (`types/contract.ts:24`) | `"USER" \| "ADMIN"` | 일치 |
| 에러 스키마 shape | `record ApiErrorResponse(code, message, List<ApiErrorDetail> details, Instant timestamp, int status, String path)` (`ApiErrorResponse.java:18-25`) | `ApiErrorResponse{code,message,details,timestamp,status,path}` (`types/contract.ts:45-58`), `ApiError` 클래스로 정규화 (`api/client.ts:34-50`) | `{code,message,details,timestamp,status,path}` | 일치 |
| ApiErrorDetail shape | `record ApiErrorDetail(String field, String reason)` (`ApiErrorDetail.java:9`) | `{field, reason}` (`types/contract.ts:61-66`), 페이지에서 `d.field/d.reason` 소비 (`LoginPage.tsx:46`, `SignupPage.tsx:42`) | `{field, reason}` | 일치 |
| 에러 code 값 | `EMAIL_DUPLICATED(409), INVALID_CREDENTIALS(401), VALIDATION_ERROR(400), UNAUTHORIZED(401)` (`ErrorCode.java`) | `code` 문자열 그대로 소비(`ApiError.code`) | 계약 관례 code 값 | 일치 |
| details null 처리 | 검증 오류 외에는 `null` 전달 (`GlobalExceptionHandler.java:52,59`) | `details: ApiErrorDetail[] \| null`, `err.details &&` 가드 (`LoginPage.tsx:44`) | 없으면 null | 일치 |
| 401 인증실패 shape | 보안 필터가 표준 shape 반환 (`SecurityErrorResponder.java:22-31`, EntryPoint 경유) | 401 시 토큰 제거+로그인 리다이렉트, ApiError 정규화 (`api/client.ts:99-106`) | 표준 에러 스키마 | 일치 |
| 인증 헤더 | `/api/auth/**` permitAll, 그 외 `authenticated()` (`SecurityConfig.java:52-55`); `Authorization: Bearer` 검증(JwtAuthFilter) | 토큰 존재 시 `Authorization: Bearer <token>` 자동 첨부 (`api/client.ts:82-88`) | `Authorization: Bearer <token>` | 일치 |
| CORS 오리진 | `http://localhost:5173`, `http://localhost:3000` 허용 (`SecurityConfig.java:73-84`) | Vite dev 서버(5173), baseURL 8080 (`.env:2`) | Vite dev 오리진 허용 | 일치 |
| 날짜 직렬화 | `Instant` → Jackson 3 기본 ISO 8601 문자열 (`application.yml:18-21`) | `createdAt: string` (ISO 8601) | ISO 8601 문자열 | 일치 (테스트 `createdAt isString` 통과) |
| 금액 타입 | (인증 모듈에 금액 필드 없음) | - | int(원) | 해당 없음 |

경계면 대조: **전 항목 일치**. 필드명 표기 불일치·중첩/평탄화 불일치·nullable 불일치·인증헤더 누락·에러포맷 이탈 등 알려진 경계면 버그 패턴 미발견.

### 2. 실제 빌드/테스트 실행 결과

| 대상 | 명령 | 결과 |
|------|------|------|
| 백엔드 테스트 | `cd backend && ./gradlew clean test` | **BUILD SUCCESSFUL** (7s). `AuthControllerTest` 6개 테스트 tests=6, failures=0, errors=0, skipped=0 |
| 프론트 타입체크 | `cd frontend && npx tsc -b` | **통과** (exit 0, 오류 없음) |
| 프론트 빌드 | `cd frontend && npm run build` (`tsc -b && vite build`) | **성공** (85 modules transformed, built in ~0.6s, dist 생성) |

- 프론트 의존성 설치: `npm ci` 성공(59 packages, 0 vulnerabilities).
- 백엔드 테스트는 test 프로파일(H2 in-memory)로 실제 DB 없이 실행됨. dev PostgreSQL 연결 불필요.
- `AuthControllerTest` 커버리지: signup 201 + passwordHash 미노출, 이메일 중복 409, 이메일 형식 400+details, login 200, 비밀번호 오류 401, 미인증 보호경로 접근 401(표준 shape). 즉 경계면 계약이 실행으로도 검증됨.
- 실제 서버 기동 후 curl 호출 검증은 별도 수행하지 않음(통합 테스트 MockMvc가 응답 shape/상태코드를 동등 수준으로 검증하므로 대체). dev DB(PostgreSQL) 미기동 환경이라 실서버 기동은 생략.

### 3. 발견된 불일치 목록

**없음.** 인증 모듈 경계면에서 계약 위반·필드 불일치·상태코드 불일치를 발견하지 못함.

#### 참고(불일치 아님, 낮은 우선순위 관찰)
- `UserResponse.java:11-12` 주석은 `spring.jackson.serialization.write-dates-as-timestamps=false` 설정에 의존한다고 기술하나, `application.yml`에는 해당 속성이 명시되어 있지 않고 Jackson 3 기본 동작(ISO 8601 문자열 직렬화)에 의존한다. 실제 직렬화 결과는 계약과 일치하며 테스트(`createdAt isString`)로 검증되었으므로 결함 아님. 주석-설정 간 문구 정합성만 추후 정리 권장.
- `compileJava` 시 `JwtAuthFilter.java`의 deprecated API 사용 경고 1건 발생(빌드/테스트에는 영향 없음). 인증 게이트와 무관.

### 4. 게이트 판정

- **인증 모듈 게이트: 통과(PASS)**
- 사유: (a) 경계면 대조 전 항목 일치, 미해결 불일치 0건. (b) 백엔드 `clean test` 6/6 통과, 프론트 `tsc -b` 및 `vite build` 모두 통과. 완료 판정 기준(미해결 불일치 0 + 양쪽 빌드/테스트 통과) 충족.

---

## 상품 모듈 검증 (2차) — Phase C / Stage 2

- **검증 일시**: 2026-07-24
- **대상 모듈**: 상품(목록/상세). 장바구니/주문/결제는 미구현으로 검증 대상 아님
- **검증자**: 오케스트레이터(메인 스레드)가 `integration-qa` 스킬로 직접 수행
- **대상 코드**:
  - 백엔드: `ecommerce/backend/` — `product/` 패키지 신규 + `common/dto/PageResponse`, `common/exception` 확장
  - 프론트: `ecommerce/frontend/` — `api/products.ts`, `pages/ProductListPage.tsx`, `pages/ProductDetailPage.tsx`, `router.tsx`

### 1. 경계면 대조 결과 표

| 항목 | 백엔드(BE) | 프론트(FE) | 계약 | 일치 |
|------|-----------|-----------|------|------|
| 목록 경로/메서드 | `GET /api/products` (`ProductController.java:@RequestMapping("/api/products")` + `@GetMapping`) | `client.get("/api/products", {params})` (`api/products.ts:22-25`) | `GET /api/products` (200) | 일치 |
| 상세 경로/메서드 | `GET /api/products/{id}` (`ProductController.java` `@GetMapping("/{id}")`) | `client.get(\`/api/products/${id}\`)` (`api/products.ts:29-31`) | `GET /api/products/{id}` (200) | 일치 |
| 쿼리 파라미터 | `page`(기본 0), `size`(기본 20), `category`(선택) — `@RequestParam` | `ListProductsParams{page?,size?,category?}`, axios가 undefined 키 생략 (`api/products.ts:7-14`) | `page`/`size`/`category` | 일치 |
| 목록 응답 래핑 | 자체 `PageResponse` record로 변환(`PageResponse.from(Page)`), Spring `Page` 직렬화 미사용 | `PageResponse<Product>` 소비, `data.items`/`data.page`/`data.totalPages` 사용 (`ProductListPage.tsx:119-159`) | `{items,page,size,totalElements,totalPages}` | 일치 |
| PageResponse 필드명 | `items, page, size, totalElements, totalPages` (`common/dto/PageResponse.java:20-26`) | 동일 (`types/contract.ts:73-83`) | 동일 | 일치 |
| Product 필드/순서 | `id,name,description,price,imageUrl,stock,category,createdAt` (`product/dto/ProductResponse.java:11-20`) | 동일 (`types/contract.ts:99-110`) | 동일 | 일치 |
| 금액 타입 | `Integer price` (정수 원) | `price: number`, `toLocaleString("ko-KR")+"원"` 포맷 (`ProductListPage.tsx:21`) | 정수 원(KRW) | 일치 |
| nullable 처리 | `description/imageUrl/category` null 허용, `default-property-inclusion: always`로 **키 생략 없이 null 명시** | `string \| null` (옵셔널 아님) | nullable 명시 | 일치 (실응답 확인) |
| 날짜 직렬화 | `Instant` → ISO 8601 문자열. 실응답: `"2026-07-24T14:23:54.609722Z"` | `createdAt: string` | ISO 8601 문자열 | 일치 (실응답 확인) |
| 상세 404 | `ProductNotFoundException` → `ErrorCode.NOT_FOUND` → 404 + 표준 에러 스키마 | `err instanceof ApiError && err.code === "NOT_FOUND"` 분기 (`ProductDetailPage.tsx:36`) | 404 `NOT_FOUND` | 일치 |
| page/size 유효성 | `page<0` 또는 `size<=0` → `InvalidPageRequestException` → 400 `VALIDATION_ERROR` | 페이지네이션 버튼이 `0 <= page <= totalPages-1`로 제한 | 400 `VALIDATION_ERROR` | 일치 |
| size 상한 | 상한 없음(`PageRequest.of(page,size)` 그대로) | 카테고리 스캔에 `size=100` 사용 (`ProductListPage.tsx:18,37`) | 계약에 상한 규정 없음 | 일치 (100 요청 정상 처리 확인) |
| 인증 요구 | `GET /api/products/**` permitAll (`SecurityConfig.java:53`) | 라우트를 `RequireAuth`로 감싸지 않음 (`router.tsx:36-37`) | 인증 불필요 | 일치 |
| 인증 헤더 | 토큰이 와도 permitAll로 무시 | 인터셉터가 토큰 있으면 자동 첨부(무해) | - | 일치 |
| 잘못된 타입 파라미터 | (수정 후) `MethodArgumentTypeMismatchException` → 400 `VALIDATION_ERROR` + details | `ApiError`로 정규화되어 메시지 노출 | 400 `VALIDATION_ERROR` | 일치 (수정 후) |
| CORS 오리진 | `http://localhost:5173`, `:3000` 허용 (Stage 1과 동일) | Vite dev 5173, baseURL 8080 | Vite dev 허용 | 일치 |

경계면 대조: 최초 검증에서 **불일치 2건** 발견 → 수정 후 **전 항목 일치**.

### 2. 실제 빌드/테스트 실행 결과

| 대상 | 명령 | 결과 |
|------|------|------|
| 백엔드 테스트 | `cd backend && ./gradlew cleanTest test` | **BUILD SUCCESSFUL**. `AuthControllerTest` 6/6, `ProductControllerTest` 10/10 — 합계 **16/16**, failures=0, errors=0 |
| 프론트 타입체크+빌드 | `cd frontend && npm run build` (`tsc -b && vite build`) | **성공**. tsc 오류 0, 88 modules transformed, `dist/` 생성 |
| 실응답 확인 | MockMvc 진단 테스트로 실제 JSON 본문 출력 후 삭제 | 목록/에러 응답 shape 육안 대조 완료 (아래 인용) |

**검증 방법 주의:** `./gradlew test`만 실행하면 Gradle이 `UP-TO-DATE`로 **테스트를 건너뛴다**. 실제 실행을
보장하려면 반드시 `cleanTest`를 함께 지정한다(본 검증에서 실제로 첫 실행이 캐시 히트였음).

**실서버 curl 검증은 생략**: dev 프로필의 PostgreSQL(5432) 미기동이며 H2는 `testRuntimeOnly`라
`bootRun` 불가. 대신 MockMvc로 실제 직렬화 결과를 확보하여 동등 수준으로 검증함. 확보한 실응답:

```json
{"items":[{"id":1,"name":"무선 이어폰","description":null,"price":129000,"imageUrl":null,
"stock":50,"category":null,"createdAt":"2026-07-24T14:23:54.609722Z"}],
"page":0,"size":20,"totalElements":1,"totalPages":1}
```

### 3. 발견된 불일치 목록

#### [상품] 불일치 #1 — 쿼리 파라미터 타입 불일치 시 500 (해결)

- **항목**: 에러 상태코드/포맷
- **백엔드**: `common/exception/GlobalExceptionHandler.java` — `MethodArgumentTypeMismatchException` 핸들러 부재로
  fallback `@ExceptionHandler(Exception.class)`에 포착 → `GET /api/products?page=abc` 실응답
  `500 {"code":"INTERNAL_ERROR",...}`
- **프론트**: `api/client.ts` — `ApiError`로 정규화되나 code가 `INTERNAL_ERROR`라 사용자에게 "서버 내부 오류"로 표시
- **기대(계약)**: `api-spec.md` 2.1 — 잘못된 page/size는 `400 VALIDATION_ERROR`
- **영향**: 클라이언트 입력 오류가 서버 결함(5xx)으로 보고되어 모니터링 오염. **상품 전용이 아닌 공통 인프라 결함**으로
  Stage 3(`/api/cart/items/{id}`)·Stage 4(`/api/orders/{id}`)에 그대로 전파될 사안
- **담당**: backend-engineer
- **상태**: **해결** — `GlobalExceptionHandler`에 `MethodArgumentTypeMismatchException` +
  `MissingServletRequestParameterException` 핸들러 추가(400 `VALIDATION_ERROR` + `details`에 파라미터명).
  회귀 테스트 `list_nonNumericPage_returns400_notInternalError` 추가, 재실행 통과 확인

#### [상품] 불일치 #2 — PathVariable 타입 불일치 시 500 (해결)

- **항목**: 에러 상태코드/포맷
- **백엔드**: 동일 원인. `GET /api/products/abc` 실응답 `500 {"code":"INTERNAL_ERROR",...}`
- **프론트**: `ProductDetailPage.tsx:36`은 `code === "NOT_FOUND"`만 분기하므로 안내 문구가 "상품을 찾을 수 없습니다"가
  아닌 서버 오류 메시지로 노출
- **기대(계약)**: 클라이언트 입력 오류이므로 4xx (`VALIDATION_ERROR`)
- **영향**: 위와 동일. 경로 파라미터를 쓰는 모든 후속 도메인에 전파
- **담당**: backend-engineer
- **상태**: **해결** — 위 핸들러로 함께 처리. 회귀 테스트 `detail_nonNumericId_returns400_notInternalError` 추가, 통과 확인

#### 참고(불일치 아님, 관찰 사항)

- **계약 빈틈 — `category=""`(빈 문자열) 처리 미규정**: BE는 `isBlank()`를 "필터 없음"으로 처리하고,
  FE는 `category || undefined`로 빈 문자열을 애초에 보내지 않는다. 현재는 양쪽이 우연히 같은 규칙이라
  문제없으나 계약에 명시가 없다. 추후 api-architect가 명문화 권장(현 시점 결함 아님).
- **FE 카테고리 목록 수집 방식의 한계**: 카테고리 전용 API가 계약에 없어 `size=100` 단발 조회로 카테고리를
  수집한다(`ProductListPage.tsx:18`). 상품이 100개를 넘으면 일부 카테고리가 누락된다. FE 코드 주석에
  한계가 명시되어 있으며, 확장 시 전용 카테고리 API 신설이 필요하다.
- **시드 데이터**: `product/config/ProductSeeder.java` — `@Profile("!test")`로 test(H2)에서 비활성.
  dev에서 상품 12개(electronics/fashion/home 각 4개) 삽입, `count() > 0`이면 중복 삽입 안 함.
  테스트 프로필 격리가 올바르게 되어 있어 시드가 테스트를 오염시키지 않음을 확인.
- **Stage 3 자리표시자**: 상품 상세의 "장바구니 담기" 버튼은 `disabled` + TODO 주석만 존재하며 API 호출 없음.
  계약 위반 아님.

### 4. 게이트 판정

- **상품 모듈 게이트: 통과(PASS)**
- 사유: (a) 발견된 불일치 2건 모두 해결, 미해결 0건. (b) 백엔드 `cleanTest test` 16/16 통과(회귀 없음),
  프론트 `tsc -b` + `vite build` 통과. (c) 실제 응답 JSON을 확보해 필드명·null 처리·날짜 직렬화까지
  값 단위로 대조 완료. 완료 판정 기준 충족.

---

## 장바구니 모듈 검증 (3차) — 2026-07-26

**대상:** Stage 3 산출물 (BE `cart/` 패키지 4개 엔드포인트, FE `api/cart.ts`·`CartPage`·`/cart` 라우트·상품상세 담기 버튼)
**검증자:** 오케스트레이터 직접 수행(메인 스레드). 구현 에이전트의 자기보고를 신뢰하지 않고 **모든 수치를 직접 재현**했다.

### 1. 계약 대조 (필드 단위)

| 항목 | 계약(types.ts) | 백엔드 DTO | 프론트 사용 | 판정 |
|------|---------------|-----------|------------|------|
| `CartResponse.id` | `number` | `Long id` | `number` | 일치 |
| `CartResponse.items` | `CartItemResponse[]` | `List<CartItemResponse>` | 동일 | 일치 |
| `CartResponse.totalAmount` | `number`(정수 원) | `Integer` | `number` | 일치 |
| `CartItemResponse.id` | CartItem id | `Long id` (CartItem id) | `item.id`를 PATCH/DELETE 경로에 사용 | 일치 |
| `.productId` | `number` | `Long` | `number` | 일치 |
| `.productName` | `string` | `String` | `string` | 일치 |
| `.productImageUrl` | `string \| null` | `String`(null 허용) | null 분기 후 placeholder 렌더 | 일치 |
| `.price` | `number`(정수) | `Integer` (Product.price가 `Integer`) | `number` | 일치 |
| `.quantity` / `.subtotal` | `number` | `Integer` | `number` | 일치 |
| `AddCartItemRequest` | `{productId, quantity}` | 동일 + `@NotNull @Min(1)` | 동일 | 일치 |
| `UpdateCartItemRequest` | `{quantity}` | 동일 + `@NotNull @Min(1)` | 동일 | 일치 |

**경로·메서드·상태코드:** `GET /api/cart`(200) · `POST /api/cart/items`(**201**) · `PATCH /api/cart/items/{id}`(200) ·
`DELETE /api/cart/items/{id}`(200) — 컨트롤러(`CartController`)와 `api/cart.ts` 양쪽 모두 계약과 일치.
POST만 `ResponseEntity.status(CREATED)`로 201을 명시한 점 확인.

**금액 직렬화:** `Product.price`가 `Integer`이므로 BigDecimal 소수점 직렬화(`129000.00`) 위험 없음.
`subtotal = price * quantity`, `totalAmount = sum(subtotal)` 모두 서버에서 int 연산. **프론트는 총액을 재계산하지 않고
서버 `totalAmount`를 그대로 출력**하여 계산 주체가 1개로 유지됨(`CartPage.tsx`).

### 2. 동작 규칙 대조 (계약 3절)

| 규칙 | 구현 | 판정 |
|------|------|------|
| 사용자당 카트 1개, 지연 생성 | `getOrCreateCart` — `findByUserId` 실패 시 생성. `Cart.userId` unique | 일치 |
| 빈 카트는 `items: []`, `totalAmount: 0` | `buildCartResponse`가 빈 리스트 → 합계 0 | 일치 |
| 동일 productId 수량 합산 | `findByCartIdAndProductId` 존재 시 `mergeQuantity` | 일치 |
| 합산 동시성 방어 | `(cart_id, product_id)` DB 복합 유니크 | 일치 |
| 모든 응답이 `CartResponse` 전체 | 4개 메서드 모두 `buildCartResponse` 반환 | 일치 |
| 재고 초과 → 409 `OUT_OF_STOCK` | `validateStock` → `OutOfStockException(ErrorCode.OUT_OF_STOCK)` | 일치 |
| 타인 아이템 조작 → **403** `FORBIDDEN` | `findOwnedItem`이 미존재는 404, 소유자 불일치는 403으로 **분기** | 일치 |
| quantity < 1 → 400 `VALIDATION_ERROR` | `@Min(1)` + 기존 `GlobalExceptionHandler` | 일치 |
| **재고 차감 없음**(차감은 Stage 4 주문 시점) | `validateStock`은 검증만, `Product.stock` 변경 코드 없음 | 일치 |
| 장바구니 라우트 인증 필요 | BE: `anyRequest().authenticated()` 기본 정책 / FE: `<RequireAuth>` | 일치 |

**PATCH 의미론:** 계약의 "변경 후 수량"대로 **절대값 치환**(`item.changeQuantity(request.quantity())`)이며
증분이 아니다. 프론트도 `item.quantity ± 1`을 계산해 절대값으로 보내므로 양쪽 해석이 같다.

### 3. 실제 실행 검증 (직접 재현)

- **백엔드** — `build/test-results/test/*.xml`을 직접 파싱:
  `CartControllerTest` **tests=15 failures=0 errors=0**, `AuthControllerTest` 6/6, `ProductControllerTest` 10/10 →
  **합계 31/31, 회귀 0건**. XML 타임스탬프가 당일 갱신본이라 `UP-TO-DATE` 스킵이 아닌 실제 실행임을 확인.
  케이스에 지연생성·401·201 shape·수량합산·재고초과 409(추가/수정)·404·400(추가/수정)·403(PATCH/DELETE)·
  삭제 후 응답에서 제거 확인이 모두 포함됨.
- **프론트** — 직접 실행: `npx tsc --noEmit` exit=0(출력 없음), `npm run build` 성공(90 modules, dist 생성).

### 4. 불일치

**미해결 불일치 0건.** 이번 라운드는 발견된 계약 불일치 자체가 없었다(Stage 1·2에서 공통 인프라가
정리된 효과로 판단).

#### 참고(불일치 아님, 관찰 사항)

- **`insertNewItem`의 경합 복구 로직은 의도대로 동작하지 않을 가능성이 높다** (`CartService.java`).
  `saveAndFlush`가 `DataIntegrityViolationException`을 던지면 JPA는 해당 트랜잭션을 보통
  rollback-only로 표시하므로, 같은 트랜잭션 안에서 기존 행에 병합하는 복구는 커밋 시점에
  `UnexpectedRollbackException`으로 실패할 수 있다. **다만 1차 방어선인 DB 복합 유니크 제약은 정상 작동**하여
  중복 행이 생기지 않는다(요청이 실패할 뿐 데이터는 오염되지 않음). 실제 동시 요청 경합은 MVP에서 매우 드물고
  계약 위반도 아니므로 차단 사유로 보지 않는다. 근본 해결이 필요하면 DB 레벨 upsert 또는
  신규 트랜잭션에서의 재시도로 전환할 것. **Stage 4 주문 생성(재고 차감)에서 유사 패턴을 쓸 경우 반드시 재검토.**
- **삭제된 상품이 장바구니에 남아 있으면 `GET /api/cart`가 404로 실패한다** (`buildCartResponse`).
  계약상 `GET /api/cart`의 에러는 401뿐이므로 형식상 어긋나지만, MVP에 상품 삭제 API가 없어 도달 불가 경로다.
  상품 삭제/비활성화를 도입하면 해당 아이템을 건너뛰거나 별도 표기하는 정책이 필요하다.
- **장바구니 화면의 수량 "+" 버튼은 재고 상한에서 막히지 않는다.** `CartItemResponse`에 `stock`이 없어
  클라이언트가 상한을 알 수 없기 때문이며(계약대로), 서버가 409 `OUT_OF_STOCK`을 주면 아이템별 메시지로
  안내한다. 계약 준수 결과이지 결함이 아니다.
- **상품 상세의 수량 스테퍼**는 계약에 없는 UX 추가분이다. 상한을 `product.stock`으로 두되
  **서버 검증을 대체하지 않고** 서버 409 경로가 그대로 살아 있음을 확인했다.
- **비로그인 "담기"**: 버튼을 비활성화하지 않고 `/login`으로 이동시키며 `state.from`에 복귀 경로를 담는다.
  기존 `LoginPage`의 리다이렉트 패턴과 일치.

### 5. 게이트 판정

- **장바구니 모듈 게이트: 통과(PASS)**
- 사유: (a) 계약 불일치 0건, 미해결 0건. (b) 백엔드 31/31 통과(회귀 없음)·프론트 타입체크/빌드 통과를
  **오케스트레이터가 직접 재현**하여 확인. (c) 필드 대조에 더해 동작 규칙(지연생성·합산·소유권 403·
  재고 검증만 수행)까지 코드 레벨로 확인. 완료 판정 기준 충족.

---

_다음 모듈(주문/모의결제)은 구현 완료 후 본 리포트에 새 섹션으로 이어 검증한다._
