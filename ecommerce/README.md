# 이커머스 MVP — 실행 가이드

React(Vite) + Spring Boot 기반 이커머스 MVP. 상품 · 장바구니 · 인증 · 주문 · 모의결제로 구성된다.

API 계약(단일 기준)은 `_workspace/contract/` 에 있고, 빌드 진행 이력은 `_workspace/PROGRESS.md` 에 있다.

---

## 사전 요구사항

| 도구 | 버전 | 비고 |
|------|------|------|
| JDK | **25** | Spring Boot 4.0.x 요구. `java -version` 으로 확인 |
| Node.js | 22 이상 | |
| Docker | 임의 | PostgreSQL 컨테이너 기동용 |

> Gradle은 별도 설치가 필요 없다(wrapper 포함).

---

## 기동 순서

세 개를 순서대로 띄운다. 각각 별도 터미널을 쓰거나 백그라운드로 실행한다.

### 1. PostgreSQL

```bash
cd ecommerce
docker compose up -d
```

`application.yml` 의 dev 데이터소스(`localhost:5432`, `ecommerce/ecommerce/ecommerce`)와 값이 맞춰져 있다.
컨테이너가 질의를 받을 준비가 됐는지는 헬스체크로 확인한다.

```bash
docker compose ps        # STATUS 가 healthy 가 될 때까지 대기
```

### 2. 백엔드 (포트 8080)

```bash
cd ecommerce/backend
./gradlew bootRun
```

- `Started EcommerceApplication` 로그가 나오면 준비 완료.
- 스키마는 `ddl-auto: update` 로 자동 생성된다.
- 첫 기동 시 `ProductSeeder` 가 상품 12개(electronics / fashion / home)를 시드한다.
  이미 상품이 있으면 건너뛴다.

### 3. 프론트엔드 (포트 5173)

```bash
cd ecommerce/frontend
npm install     # 최초 1회
npm run dev
```

브라우저에서 <http://localhost:5173> 로 접속한다.

- API 주소는 `frontend/.env` 의 `VITE_API_BASE_URL` 로 주입된다(기본 `http://localhost:8080`).
- 프론트가 백엔드를 **절대 URL로 직접 호출**하므로 Vite 프록시는 쓰지 않는다.
  대신 백엔드 `SecurityConfig` 가 `localhost:5173` · `localhost:3000` 오리진을 CORS 허용한다.
  **프론트 포트를 바꾸면 `SecurityConfig.corsConfigurationSource()` 의 허용 오리진도 함께 고쳐야 한다.**

---

## 종료

```bash
# 백엔드 / 프론트엔드는 각 터미널에서 Ctrl+C

cd ecommerce
docker compose down          # 컨테이너만 정리 (데이터 보존)
docker compose down -v       # 데이터까지 삭제 (시드부터 다시 시작)
```

---

## 검증

### 단위 · 통합 테스트 (백엔드)

```bash
cd ecommerce/backend
./gradlew cleanTest test
```

> **`./gradlew test` 만 실행하면 안 된다.** 입력이 바뀌지 않으면 Gradle이 `UP-TO-DATE` 로
> 테스트를 통째로 건너뛰므로 통과 여부를 실제로 확인할 수 없다. 반드시 `cleanTest` 를 함께 준다.

테스트는 H2(in-memory)로 돌고, `test` 프로파일에서는 `ProductSeeder` 가 비활성화된다.

### E2E 스모크 (실제 구동 기준)

BE·FE가 아니라 **PostgreSQL + 실제 HTTP 스택**을 상대로 계약 전 구간을 검사한다.
백엔드가 떠 있는 상태에서 실행한다.

```bash
cd ecommerce
./_workspace/e2e-smoke.sh
```

인증 → 상품 → 장바구니 → 주문 → 모의결제 + 공통 에러 스키마 · CORS까지 84개 항목을 확인하고
마지막에 `통과 N / 실패 M` 을 출력한다. 실패가 있으면 항목별로 기대값과 실제값을 함께 보여준다.

통합 테스트가 덮지 못하는 아래 층을 이 스크립트가 담당한다.

- **커밋된 트랜잭션** — 통합 테스트는 롤백되므로 재고 차감이 실제로 영속되는지 확인하지 못한다.
- **CORS 프리플라이트** — MockMvc는 필터 체인을 슬라이스로 태워 검증되지 않는다.
- **PostgreSQL 방언** — 테스트는 H2로 돌기 때문에 DDL·쿼리 차이가 드러나지 않는다.

### 프론트엔드 빌드

```bash
cd ecommerce/frontend
npx tsc --noEmit
npm run build
```

---

## 트러블슈팅

| 증상 | 원인 / 조치 |
|------|------------|
| 백엔드가 `Connection refused` 로 죽는다 | PostgreSQL 컨테이너가 아직 healthy 가 아니다. `docker compose ps` 로 확인 후 재기동 |
| 브라우저 콘솔에 CORS 오류 | 프론트 포트가 5173/3000이 아니다. `SecurityConfig` 의 허용 오리진에 추가 |
| 로그인은 되는데 장바구니가 401 | 토큰이 localStorage에 없다. 로그아웃 후 재로그인 |
| 상품 목록이 비어 있다 | 시더는 상품이 0건일 때만 동작한다. `docker compose down -v` 로 초기화 후 재기동 |
| `./gradlew test` 가 항상 통과한다 | `UP-TO-DATE` 로 건너뛴 것이다. `cleanTest test` 를 쓸 것 |
