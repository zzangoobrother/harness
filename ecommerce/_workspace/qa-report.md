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

_다음 모듈(상품 → 장바구니 → 주문/결제)은 구현 완료 후 본 리포트에 새 섹션으로 이어 검증한다._
