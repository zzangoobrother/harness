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

---

## 주문/결제 모듈 검증 (4차) — 2026-07-27

**대상:** Stage 4 산출물
- BE: `order/`(controller·dto·entity·mapper·repository·service), `payment/`(동일 구성),
  `product/repository/ProductRepository.java`(재고 차감), `common/exception/` 신규 3개
  (`AlreadyPaidException`, `CartEmptyException`, `OrderNotFoundException`)
- FE: `api/orders.ts`, `pages/CheckoutPage.tsx`, `pages/OrderHistoryPage.tsx`,
  `pages/OrderCompletePage.tsx`, `components/OrderStatusBadge.tsx`, `utils/format.ts`,
  `router.tsx`, `components/Layout.tsx`, `pages/CartPage.tsx`, `types/contract.ts`

**기준 계약:** `_workspace/contract/api-spec.md` 4·5절, `data-model.md` §5~7, `types.ts`
**검증자:** qa-integrator. 파일 존재 여부가 아니라 **BE 응답 DTO ↔ FE 소비 타입을 필드 단위로 대조**하고,
백엔드 테스트·프론트 타입체크/빌드를 **직접 실행**해 수치를 재현했다.

### 1. 응답 shape 대조 (필드 단위)

| 필드 | 계약(`types.ts`) | 백엔드 DTO | 프론트 소비 | 판정 |
|------|-----------------|-----------|------------|------|
| `OrderResponse.id` | `number` | `Long id` (`order/dto/OrderResponse.java:16`) | `order.id` (`OrderCompletePage.tsx:150`) | 일치 |
| `.status` | `OrderStatus` | `OrderStatus`(`@Enumerated(STRING)`, `OrderResponse.java:17`) | `Record<OrderStatus,string>` 키로 소비 (`OrderStatusBadge.tsx:6-18`) | 일치 |
| `.totalAmount` | `number`(정수 원) | `Integer` (`OrderResponse.java:18`) | `formatPrice(order.totalAmount)` (`OrderCompletePage.tsx:170`) | 일치 |
| `.items` | `OrderItemResponse[]` | `List<OrderItemResponse>` (`OrderResponse.java:19`) | `order.items.map` (`OrderCompletePage.tsx:158`) | 일치 |
| `.payment` | `PaymentResponse \| null` | `PaymentResponse payment` (`OrderResponse.java:20`) — 결제 전 `null` 전달 (`OrderMapper.java:36-43`, `OrderService.java:116,187`) | `{order.payment && (...)}` null 가드 (`OrderCompletePage.tsx:177`) | 일치 |
| `.createdAt` | `string`(ISO 8601) | `Instant` (`OrderResponse.java:21`) | `formatDateTime(order.createdAt)` (`utils/format.ts:11-22`) | 일치 |
| `OrderItemResponse.id` | OrderItem id | `Long id` (`OrderItemResponse.java:8`) | `key={item.id}` | 일치 |
| `.productId` | `number` | `Long` (`OrderItemResponse.java:9`) | (표시 미사용, 타입만 소비) | 일치 |
| **`.productName`** | `string` | `String` — 엔티티에 없어 `OrderService`가 `Product`를 일괄 조회해 주입 (`OrderMapper.java:22-31`, `OrderService.java:111-114,183-186`) | `{item.productName}` (`OrderCompletePage.tsx:160`, `OrderHistoryPage.tsx:25`) | 일치 |
| **`.priceAtOrder`** | `number`(스냅샷) | `Integer` — 체크아웃 시 `product.getPrice()` 복사 (`OrderService.java:103-104`) | `formatPrice(item.priceAtOrder)` (`OrderCompletePage.tsx:162`) | 일치 |
| **`.subtotal`** | `number` | `Integer` — `OrderItem.subtotal()` = `priceAtOrder * quantity`, DB 미저장·응답 계산 (`OrderItem.java:55-57`) | `formatPrice(item.subtotal)` (`OrderCompletePage.tsx:164`) | 일치 |
| `PaymentResponse.id/orderId/amount` | `number` | `Long/Long/Integer` (`PaymentResponse.java:13-15`) | `order.payment.amount` 표시 | 일치 |
| `.status` | `PaymentStatus` | `PaymentStatus`(STRING) (`PaymentResponse.java:16`) | `Record<PaymentStatus,string>` (`OrderStatusBadge.tsx:20-30`), `paymentResult?.status === "SUCCESS"` (`OrderCompletePage.tsx:141`) | 일치 |
| `.method` | `"MOCK"` | `PaymentMethod`(STRING) (`PaymentResponse.java:17`) | `payment.method === "MOCK" ? "모의결제" : ...` (`OrderCompletePage.tsx:191`) | 일치 |
| **`.paidAt`** | `string \| null` | `Instant paidAt` — 실패/대기 시 `null` (`Payment.java:69-72`) | `payment.paidAt ? formatDateTime(...) : "-"` (`OrderCompletePage.tsx:195`) | 일치 |

**null 필드 보존:** `application.yml`의 `default-property-inclusion: always`로 `payment`/`paidAt`가
**키 생략 없이 `null`로 직렬화**된다. 테스트가 `doesNotExist()`가 아니라 원문을 파싱해
"필드 존재 + 값 null"을 분리 검증한다(`OrderControllerTest.java:429-441`) — 계약이 명시한 null 필드가
조용히 사라지는 흔한 경계면 버그를 정확히 막고 있다.

**camelCase:** BE에 `@JsonNaming`/`PropertyNamingStrategy` 설정이 없어 record 컴포넌트명이 그대로
JSON 키가 된다. 6개 DTO 전 필드가 camelCase로 계약과 동일.

**날짜/금액 직렬화:** `Instant` → ISO 8601 문자열(테스트 `$.createdAt isString`, `$.paidAt isString`로 확인),
금액은 전부 `Integer`라 소수점(`258000.00`) 직렬화 위험 없음.

**목록 래핑:** `GET /api/orders`는 `ResponseEntity<List<OrderResponse>>`로 **배열** 직렬화
(`OrderController.java:46-49`). FE도 `client.get<OrderResponse[]>` 후 `res.data`를 배열로 받아
`.content`/`.items` 접근이 **없다**(`api/orders.ts:30-33`, `OrderHistoryPage.tsx:39`).
테스트 `$ isArray` + `$.length()`로 실제 직렬화 형태까지 확인(`OrderControllerTest.java:176-177`).

### 2. 경로·메서드·상태코드 대조

| 엔드포인트 | 계약 | 백엔드 | 프론트 | 판정 |
|-----------|------|--------|--------|------|
| `POST /api/orders` | **201** | `ResponseEntity.status(HttpStatus.CREATED)` (`OrderController.java:43`) | `client.post("/api/orders", {})` (`api/orders.ts:24-26`) | 일치 |
| `GET /api/orders` | 200 | `ResponseEntity.ok` (`OrderController.java:48`) | `client.get("/api/orders")` (`api/orders.ts:31`) | 일치 |
| `GET /api/orders/{id}` | 200 | `ResponseEntity.ok` (`OrderController.java:54`) | `client.get(\`/api/orders/${id}\`)` (`api/orders.ts:37`) | 일치 |
| `POST /api/orders/{id}/payment` | **200** | `ResponseEntity.ok` (`PaymentController.java:38`) | `client.post(\`/api/orders/${id}/payment\`)` (`api/orders.ts:49`) | 일치 |

`PaymentController`가 `@RequestMapping("/api/orders")` + `@PostMapping("/{id}/payment")`로 매핑되어
결제 경로가 주문 하위임에도 payment 패키지에 위치한다 — 경로 문자열은 계약과 동일.

### 3. 결제 실패 경로 (가장 중요)

| 검증 항목 | 결과 | 근거 |
|----------|------|------|
| BE: `simulateSuccess=false` → 예외를 던지지 않는가 | **예외 없음** | `PaymentService.java:60-67` — if/else 분기만 존재. `AlreadyPaidException`은 SUCCESS 재결제 경로에서만(`:51-53`) |
| BE: HTTP 200 + `status=FAILED` + `paidAt=null` | **성립** | `PaymentController.java:38`이 무조건 `ok()`. `Payment.markFailed()`가 `paidAt=null`로 되돌림(`Payment.java:69-72`). 테스트 `pay_failure_returns200WithFailedStatus_notAnError`가 200·`status=FAILED`·`paidAt` 존재+null까지 검증(`OrderControllerTest.java:298-322`) |
| BE: 실패 시 `Order.status=FAILED` 전이 | **성립** | `order.markFailed()` (`PaymentService.java:66`), 테스트가 후속 `GET`으로 `$.status=FAILED`·`$.payment.status=FAILED` 확인 |
| FE: try/catch가 아니라 응답 본문 `status`로 분기하는가 | **본문 `status`로 분기** | `OrderCompletePage.tsx:82` `setPaymentResult(payment)` → `:141` `paymentResult?.status === "SUCCESS"`. catch 블록(`:92-95`)은 401/403/404/409만 처리 |
| FE: 실패를 "시스템 에러"로 오표시하지 않는가 | **오표시 없음** | 실패는 `role="status"`의 결과 표시(`:201-210`)이며 `alert-error`(`:212-216`)가 아님. 문구도 "모의결제가 실패했습니다. 아래에서 다시 시도할 수 있습니다."로 재시도 가능함을 안내 |
| 양쪽이 `ALREADY_PAID`(409)를 **SUCCESS 주문에만** 적용하는가 | **일치** | BE `payment.getStatus() == PaymentStatus.SUCCESS`일 때만 409(`PaymentService.java:51-53`). FE `payable = status === "PENDING" \|\| status === "FAILED"`(`OrderCompletePage.tsx:139`) |
| FE가 FAILED 주문 재결제를 허용하는가 | **허용** | 위 `payable` 식이 `FAILED`를 포함. BE도 재결제 허용(테스트 `pay_failedOrder_canBeRetried`, `OrderControllerTest.java:343-368`) — Payment는 orderId 유니크라 새 행이 아니라 기존 행을 갱신 |

**결론: 결제 실패 경로에서 BE·FE 해석이 완전히 일치한다.** 이 하네스에서 가장 어긋나기 쉬운 지점
(실패를 4xx로 던지거나, FE가 catch로 실패를 잡으려다 영원히 성공으로 오인하는 패턴)이 양쪽 모두 회피되었다.

### 4. 기본값 계약 (`simulateSuccess` 생략 = 성공)

| 검증 항목 | 결과 | 근거 |
|----------|------|------|
| BE `PaymentRequest.simulateSuccess`가 래퍼 `Boolean`인가 | **`Boolean`** | `PaymentRequest.java:9` — primitive `boolean`이면 기본값 false로 뒤집힌다는 위험을 주석으로 명시 |
| null → true 해석 | **성립** | `resolveSimulateSuccess()` = `simulateSuccess == null \|\| simulateSuccess` (`PaymentRequest.java:12-14`) |
| 본문 자체가 없을 때(`request == null`)도 true | **성립** | `PaymentService.java:60` `request == null \|\| request.resolveSimulateSuccess()` + `@RequestBody(required = false)` (`PaymentController.java:37`). 테스트 `pay_withNoBodyAtAll_defaultsToSuccess` |
| FE 인자 생략 시 실제 전송 본문이 `{}`인가 | **성립** | `payOrder(id, simulateSuccess?)`가 `{ simulateSuccess: undefined }`를 만들고(`api/orders.ts:48`), axios의 `JSON.stringify`가 `undefined` 키를 제거해 `{}`가 전송된다. BE가 `{}`를 null→true로 해석(테스트 `pay_withEmptyJsonObject_defaultsToSuccess`, `OrderControllerTest.java:269-283`) |
| `POST /api/orders`가 본문 `{}`로 파싱되는가 | **성립** | `record CheckoutRequest()`(컴포넌트 0개)로 `{}` 역직렬화. `@RequestBody(required=false)`라 본문 생략도 201(테스트 `checkout_withNoBodyAtAll_returns201`). FE는 `{}`를 명시 전송(`api/orders.ts:24-25`) |

즉 **"생략 = 성공"이 BE·FE·직렬화 계층을 관통해 실제로 성립**한다. (현 FE 호출부인
`OrderCompletePage.tsx:79`는 항상 boolean을 명시하므로 생략 경로는 계약 여유분으로만 남는다.)

### 5. 에러 코드 문자열 대조

| 상황 | 계약 | BE가 내리는 code / status | FE가 분기하는 문자열 | 판정 |
|------|------|--------------------------|---------------------|------|
| 빈 장바구니 체크아웃 | `CART_EMPTY` 409 | `CartEmptyException` → `ErrorCode.CART_EMPTY`(CONFLICT) (`OrderService.java:75-77`) | `err.code === "CART_EMPTY"` (`CheckoutPage.tsx:16-18`) | 일치 |
| 재고 부족 | `OUT_OF_STOCK` 409 | `OutOfStockException` → `ErrorCode.OUT_OF_STOCK`(CONFLICT) (`OrderService.java:89-91`) | `err.code === "OUT_OF_STOCK"` (`CheckoutPage.tsx:19-21`) | 일치 |
| 타인 주문 조회/결제 | **`FORBIDDEN` 403** (404 아님) | `ForbiddenException` → `ErrorCode.FORBIDDEN`(FORBIDDEN) (`OrderService.java:165-167`) | `err.code === "FORBIDDEN"` (`OrderCompletePage.tsx:22,60`) | 일치 |
| 없는 주문 | `NOT_FOUND` 404 | `OrderNotFoundException` → `ErrorCode.NOT_FOUND` (`OrderService.java:163-164`) | `err.code === "NOT_FOUND"` (`OrderCompletePage.tsx:23,58`) | 일치 |
| 이미 결제된 주문 재결제 | `ALREADY_PAID` 409 | `AlreadyPaidException` → `ErrorCode.ALREADY_PAID`(CONFLICT) (`PaymentService.java:52`) | `err.code === "ALREADY_PAID"` (`OrderCompletePage.tsx:21`) | 일치 |
| 미인증 | `UNAUTHORIZED` 401 | 보안 필터의 `JwtAuthenticationEntryPoint`가 표준 shape 반환 | 인터셉터가 401에서 토큰 제거 후 `/login` 이동 (`api/client.ts:99-106`) | 일치 |

BE 예외 3종은 모두 `BusinessException` 하위라 `GlobalExceptionHandler.handleBusiness`가
표준 `ApiErrorResponse`로 변환한다(`GlobalExceptionHandler.java:77-81`) — 특정 예외만 스프링 기본
에러 페이지로 새는 경로 없음. 테스트가 `$.code` 값을 문자열 그대로 검증(409×2, 403, 404, 401).

### 6. 인증/라우팅

| 항목 | 결과 | 근거 |
|------|------|------|
| 주문/결제 4개 엔드포인트가 인증 필요로 잡히는가 | **필요** | `SecurityConfig.java:50-56` — permitAll은 `/api/auth/**`와 `GET /api/products/**`뿐, `/api/orders**`는 `anyRequest().authenticated()`에 걸린다. 테스트 `checkout_withoutToken_returns401`이 401 + `code=UNAUTHORIZED` 확인 |
| FE `checkout` 라우트 | `<RequireAuth>` | `router.tsx:45-52` |
| FE `orders` 라우트 | `<RequireAuth>` | `router.tsx:53-60` |
| FE `orders/:id/complete` 라우트 | `<RequireAuth>` | `router.tsx:61-68` |
| Authorization 헤더 첨부 | 인터셉터 자동 | `api/client.ts:82-88` — `api/orders.ts`가 공용 `client`만 사용하고 별도 axios 호출을 하지 않음 |
| CORS | 변경 없음 | `SecurityConfig.java:73-84`, Vite dev 5173 허용(Stage 1~3과 동일) |

### 7. 부수효과 정합성

| 항목 | 결과 | 근거 |
|------|------|------|
| 체크아웃 성공 시 장바구니를 비우는가 | **비운다** | `cartItemRepository.deleteAll(cartItems)` (`OrderService.java:109`). 테스트 `checkout_emptiesCartAndDecreasesStock`이 후속 `GET /api/cart`로 `items.length=0`·`totalAmount=0` 확인 |
| FE가 비워진 이후 장바구니를 어긋난 채 보여주는가 | **아니오** | 전역 장바구니 스토어·헤더 배지가 없어 캐시된 카운트가 남지 않는다(`Layout.tsx:22-49`는 정적 링크만). `CheckoutPage`는 성공 즉시 `navigate(..., { replace: true })`로 이탈하고(`CheckoutPage.tsx:61`), `CartPage`는 마운트마다 `getCart()` 재조회(`CartPage.tsx:32-49`). 뒤로가기로 `/cart`에 와도 빈 장바구니가 표시된다 |
| 재고 차감 시점 | **주문 생성 시점**(계약 가정 2) | `decreaseStockIfAvailable`를 체크아웃 트랜잭션 안에서 호출 (`OrderService.java:86-92`). 테스트가 50→48 확인 |
| 재고 차감 원자성 | 조건부 UPDATE 1문장 | `ProductRepository.java:38-42` — `WHERE stock >= :quantity`로 검사·차감을 원자화, 갱신 0행이면 `OUT_OF_STOCK`. **3차 리포트가 경고한 "예외 잡고 같은 트랜잭션에서 복구" 패턴을 쓰지 않았다**(`ProductRepository.java:30-31` 주석이 그 지적을 명시적으로 수용). 테스트 `checkout_insufficientStock_returns409AndDecreasesNothing`이 all-or-nothing 롤백까지 확인 |
| 결제 실패 시 재고 미복원을 FE가 오해시키는 문구가 있는가 | **없음** | BE는 의도적으로 미복원(`PaymentService.java:40-44`에 근거 기술). FE 문구는 `CheckoutPage.tsx:98-100` "아래 상품으로 주문이 생성됩니다…", `OrderCompletePage.tsx:208` "모의결제가 실패했습니다. 아래에서 다시 시도할 수 있습니다." — 재고 반환·주문 취소를 시사하는 표현이 없다 |
| 목록 정렬 해석 일치 | 일치 | BE `findByUserIdOrderByCreatedAtDescIdDesc`(`OrderRepository.java:17`), FE `sortByLatest`도 createdAt desc → id desc 동일 tie-break(`OrderHistoryPage.tsx:14-19`). 서버가 이미 정렬하므로 재정렬은 무해한 방어 |

### 8. 실제 실행 검증 (직접 실행, 출력 인용)

**백엔드** — `cd ecommerce/backend && ./gradlew cleanTest test`

```
> Task :cleanTest
> Task :test
BUILD SUCCESSFUL in 7s
6 actionable tasks: 2 executed, 4 up-to-date
```

`build/test-results/test/TEST-*.xml`을 직접 파싱한 클래스별 집계:

| 테스트 클래스 | tests | failures | errors | skipped |
|--------------|-------|----------|--------|---------|
| `AuthControllerTest` | 6 | 0 | 0 | 0 |
| `ProductControllerTest` | 10 | 0 | 0 | 0 |
| `CartControllerTest` | 15 | 0 | 0 | 0 |
| `OrderControllerTest` | **19** | 0 | 0 | 0 |
| **합계** | **50** | **0** | **0** | **0** |

기존 31개(auth 6 · product 10 · cart 15) 전부 유지 — **회귀 0건**. `cleanTest`를 선행해 `UP-TO-DATE`
스킵이 아닌 실제 실행임을 보장했다(`> Task :test`가 실행 목록에 나타남).

**프론트** — `cd ecommerce/frontend && npx tsc --noEmit && npm run build`

```
=== TSC EXIT: 0 ===        (tsc --noEmit 출력 없음)
> frontend@0.0.0 build
> tsc -b && vite build
vite v8.1.5 building client environment for production...
✓ 96 modules transformed.
dist/assets/index-8GU7yNzm.js   360.39 kB │ gzip: 114.28 kB
✓ built in 107ms
```

**계약 타입 동기화** — `diff <(tail -n +4 frontend/src/types/contract.ts) _workspace/contract/types.ts` → **차이 없음**
(FE 사본 상단 3줄의 동기화 주석 제외). 즉 FE는 계약 타입을 재정의하지 않고 그대로 사용한다.

**서버 기동 호출(curl) 검증**: 로컬 PostgreSQL이 필요한 dev 프로파일이라 생략. 대신 `@SpringBootTest` +
MockMvc가 **실제 직렬화된 JSON 본문을 파싱**해 필드 존재/null/타입을 검증하므로(위 §1 참조)
정적 대조를 넘어선 실응답 확인이 이루어졌다.

### 9. 발견된 불일치 목록

**미해결 불일치 0건. 이번 라운드에서 발견된 계약 불일치 자체가 0건이다.**

체크리스트 전 항목(응답 shape 16필드 · 상태코드 4 · 결제 실패 경로 7 · 기본값 계약 5 · 에러 코드 6 ·
인증/라우팅 6 · 부수효과 6)이 모두 "일치" 판정이며, 담당 엔지니어에게 전달할 수정 지시 사항이 없다.

#### 참고(불일치 아님, 관찰 사항)

- **삭제된 상품이 과거 주문에 남아 있으면 `GET /api/orders`·`GET /api/orders/{id}`가 404로 실패한다.**
  `OrderService.requireProduct`(`OrderService.java:199-205`)가 `ProductNotFoundException`(→404 `NOT_FOUND`)을
  던지며, 이는 계약 4.2가 규정한 에러(401만)에 없다. 다만 MVP에 상품 삭제 API가 없어 **도달 불가 경로**다.
  3차 리포트의 장바구니 동일 이슈와 같은 성격이며, 상품 삭제/비활성화 도입 시 주문 이력은
  `productName`을 스냅샷으로 보관하는 방향(현재 `OrderItem`은 상품명을 저장하지 않음)이 근본 해법이다.
- **`CANCELLED` 주문에 대한 결제 가드가 BE에는 없다.** `PaymentService.pay`는 `Payment.status == SUCCESS`만
  차단하므로 이론상 `CANCELLED` 주문도 결제된다. FE는 `payable`에서 제외해 UI로 막는다
  (`OrderCompletePage.tsx:139`). 계약 가정 6대로 취소 API가 MVP 범위 밖이라 `CANCELLED` 값이 생성되는
  경로가 없어 도달 불가. 취소 API 추가 시 BE 가드도 함께 넣어야 한다.
- **`@Valid`가 `PaymentRequest`에 붙어 있으나**(`PaymentController.java:37`) 해당 record에 제약 애노테이션이
  없어 실질 무동작이다. 해롭지 않으며 향후 필드 추가 시를 대비한 배치로 보인다.
- **`utils/format.ts` 신설과 기존 페이지의 로컬 `formatPrice` 중복.** `CartPage.tsx:10-12`,
  `ProductListPage`가 여전히 자체 함수를 갖는다. 출력 결과는 동일(`toLocaleString("ko-KR") + "원"`)하고
  계약 사안이 아니므로 차단하지 않는다. 정리한다면 별도 리팩터링 과업으로.
- **`OrderStatusBadge`가 `Record<OrderStatus, string>`을 쓰는 설계**는 계약에 상태 값이 추가되면
  컴파일 에러로 누락이 드러난다(`OrderStatusBadge.tsx:6-30`). 계약 변경에 대한 방어로 적절하다.

### 10. 게이트 판정

- **주문/결제 모듈 게이트: 통과(PASS)**
- 사유: (a) 계약 불일치 0건, 미해결 0건. (b) 백엔드 `cleanTest test` **50/50 통과**(신규 19 + 기존 31,
  회귀 0건), 프론트 `tsc --noEmit` exit 0 · `vite build` 성공 — 모두 직접 실행해 재현.
  (c) 필드 대조에 더해 이번 단계의 최대 위험 지점인 **결제 실패 = HTTP 200 + 본문 status 분기**가
  BE·FE 양쪽에서 성립함을 코드와 테스트로 확인. (d) 기본값 계약(생략=성공)이 axios 직렬화 계층까지
  포함해 실제로 성립함을 확인. 완료 판정 기준 충족.

---

_Stage 4까지 4개 모듈(인증·상품·장바구니·주문/결제) 전체가 PASS. 계약 12개 엔드포인트 모두 검증 완료._
