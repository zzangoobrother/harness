---
name: ecommerce-build
description: 이커머스 웹사이트(React + Spring Boot MVP)를 에이전트 팀으로 구축·확장·수정하는 오케스트레이터. 상품/장바구니/회원가입·로그인/주문/모의결제 기능을 만들거나, 기존 이커머스 코드를 "다시 구현/수정/보완/추가/개선"할 때 반드시 이 스킬을 사용하라. api-architect·backend-engineer·frontend-engineer·qa-integrator 4개 에이전트를 계약설계→병렬구현→경계면검증 순으로 조율한다. "이커머스 만들어줘", "상품/장바구니/결제 기능 추가", "주문 로직 수정", "프론트/백엔드 붙여줘" 같은 요청이 트리거다. 단순 단일 파일 질문·설명은 이 스킬 없이 직접 응답 가능.
---

# 이커머스 빌드 오케스트레이터

React(프론트) + Spring Boot(백엔드) 기반 이커머스 MVP를 **에이전트 팀**으로 구축한다.
개별 에이전트가 "무엇을 어떻게" 하는지는 각자의 스킬에 있고, 이 오케스트레이터는
**"누가 언제 어떤 순서로 협업하는가"**를 정의한다.

## 팀 구성

| 에이전트 | 타입/모델 | 스킬 | 역할 |
|----------|-----------|------|------|
| `api-architect` | opus | ecommerce-api-design | 데이터 모델 + REST API 계약 확정 (single source of truth) |
| `backend-engineer` | opus | spring-boot-ecommerce | Spring Boot + JPA 구현, 모의결제 |
| `frontend-engineer` | opus | react-ecommerce-ui | React + TS 구현, API 클라이언트·상태관리 |
| `qa-integrator` | opus | integration-qa | 경계면 교차 검증 (BE 응답 ↔ FE 타입) |

**모든 Agent 호출에 `model: "opus"`를 명시한다.**

## 실행 모드: 하이브리드 (에이전트 팀 기반)

| Phase | 모드 | 참여 | 데이터 전달 |
|-------|------|------|------------|
| A. 계약 설계 | 단독 | api-architect | 파일 기반 (`_workspace/contract/`) |
| B. 병렬 구현 | 에이전트 팀 | backend-engineer + frontend-engineer | 파일(계약) + 메시지(계약변경 조율) |
| C. 경계면 검증 | 생성-검증 | qa-integrator | 파일(코드 대조) + 메시지(불일치 리포트) |

## Phase 0: 컨텍스트 확인 (매 실행 시작 시 필수)

작업 시작 전, 기존 산출물을 확인하여 실행 모드를 판별한다:

- `_workspace/` **미존재** → **초기 실행** (Phase A부터 전체 진행)
- `_workspace/` **존재 + 부분 수정 요청**(예: "장바구니만 다시", "결제 로직 수정") → **부분 재실행**
  (해당 모듈 담당 에이전트만 재호출, 계약 변경 시에만 api-architect 선행)
- `_workspace/` **존재 + 새 요구 추가**(예: "리뷰 기능 추가") → **확장 실행**
  (api-architect가 계약에 신규 리소스 추가 → 담당 엔지니어 구현 → qa 검증)

기존 코드가 있으면 각 에이전트는 이전 산출물을 읽고 **개선점/변경점만** 반영한다(전면 재작성 금지).

## Phase A: 계약 설계

**모드:** api-architect 단독 호출 (`model: "opus"`)

1. `api-architect`를 호출하여 데이터 모델 + REST API 계약을 확정한다.
2. 산출물: `_workspace/contract/data-model.md`, `_workspace/contract/api-spec.md`, `_workspace/contract/types.ts`
3. **게이트:** 계약 3개 파일이 모두 생성되고 MVP 엔드포인트(인증/상품/장바구니/주문/모의결제)를 모두 포함하는지 확인한 뒤에만 Phase B로 진행한다.

> Why: 계약은 BE·FE의 유일한 진실의 원천이다. 계약이 불완전한 채 병렬 구현에 들어가면
> 통합 시점에 필드명·경로·타입 충돌이 폭증한다.

## Phase B: 병렬 구현

**모드:** 에이전트 팀 (`TeamCreate` → `backend-engineer`, `frontend-engineer`)

1. `TeamCreate`로 두 엔지니어 팀을 구성한다.
2. `TaskCreate`로 모듈 단위 작업을 부여한다. 권장 모듈 순서(의존성 기준):
   **인증 → 상품 → 장바구니 → 주문/모의결제**
3. 두 엔지니어는 `_workspace/contract/`를 읽고 각자 병렬 구현한다:
   - `backend-engineer` → `ecommerce/backend/` (Spring Boot 프로젝트)
   - `frontend-engineer` → `ecommerce/frontend/` (Vite React 프로젝트)
4. **계약 변경 프로토콜:** 구현 중 계약 수정이 필요하면 임의로 바꾸지 말고,
   `api-architect`에게 `SendMessage`로 협의 → 계약 파일 갱신 → 상대 엔지니어에게 통지.
5. **점진 완료 신호:** 각 모듈이 완성될 때마다 `TaskUpdate`로 완료 표시하여
   qa-integrator가 즉시 검증에 착수할 수 있게 한다.

> Why: 계약이라는 공유 파일 하나만 보면 두 엔지니어가 서로를 기다리지 않고 병렬 작업할 수 있다.
> 메시지는 "계약이 바뀌었다"는 예외 상황에만 쓴다.

## Phase C: 경계면 검증 (점진적)

**모드:** 생성-검증 (`qa-integrator`, `model: "opus"`)

1. **전체 완성 후 1회가 아니라**, 각 모듈이 Phase B에서 완료될 때마다 `qa-integrator`를 호출한다.
2. qa-integrator는 백엔드 응답 DTO와 프론트 API 클라이언트/TS 타입을 **동시에 읽고 대조**하고,
   백엔드 빌드/테스트·프론트 타입체크/빌드를 실제 실행한다.
3. 불일치 발견 시 `_workspace/qa-report.md`에 파일:라인과 함께 기록하고,
   담당 엔지니어에게 `SendMessage`로 전달 → 수정 → 재검증.
4. **게이트:** qa-report에 미해결 불일치가 없을 때 해당 모듈을 완료 처리한다.

## 데이터 전달 프로토콜

- **파일 기반(핵심):** 모든 중간 산출물은 `_workspace/` 하위에 저장한다.
  - `_workspace/contract/` — 계약 (data-model.md, api-spec.md, types.ts)
  - `_workspace/qa-report.md` — 검증 결과
- **메시지 기반:** 계약 변경 통지, QA 불일치 리포트 등 실시간 조율.
- **태스크 기반:** 모듈 단위 진행상황·의존관계 추적.
- 최종 산출물(`ecommerce/backend/`, `ecommerce/frontend/`)만 프로젝트 경로에 두고,
  `_workspace/`는 감사 추적용으로 보존한다.

## 에러 핸들링

- 에이전트 실패 시 **1회 재시도**. 재실패하면 해당 산출물 없이 진행하되 최종 보고에 누락을 명시한다.
- 계약과 구현이 상충하면 **계약을 우선**한다. 구현을 계약에 맞추거나, 계약이 틀렸으면 api-architect가 계약을 고친 뒤 재구현한다.
- QA가 반복적으로(2회+) 같은 유형의 불일치를 보고하면, 근본 원인(계약 모호성)을 api-architect에 피드백하여 계약 자체를 명확화한다.
- 빌드 실패는 "완료"로 간주하지 않는다. qa-integrator의 실제 컴파일/빌드 통과가 완료 조건이다.

## 테스트 시나리오

**정상 흐름:**
"이커머스 사이트 만들어줘" → Phase 0(초기 실행 판별) → Phase A(api-architect가 계약 3파일 생성)
→ Phase B(BE·FE 팀이 인증→상품→장바구니→주문/결제 병렬 구현) → Phase C(모듈별 qa 검증, 빌드 통과)
→ 최종 보고 + 피드백 요청.

**에러 흐름:**
Phase C에서 qa-integrator가 "프론트는 `productId`, 백엔드 응답은 `product_id`" 불일치를 발견
→ qa-report 기록 → backend-engineer에 SendMessage → 계약(camelCase) 확인 후 백엔드 직렬화 수정
→ 재검증 통과 → 모듈 완료.

**부분 재실행 흐름:**
"결제 실패 케이스도 처리해줘" → Phase 0(부분 재실행 판별) → api-architect가 payment 계약에 실패응답 추가
→ backend-engineer가 PaymentService 수정 → qa-integrator가 결제 모듈만 재검증.

## 실행 후 (Phase 7 진화)

빌드 완료 후 사용자에게 피드백을 요청한다("팀 구성이나 결과에서 바꾸고 싶은 점이 있나요?").
피드백 유형에 따라 수정 대상을 정하고(스킬/에이전트정의/오케스트레이터),
변경은 반드시 프로젝트 CLAUDE.md의 **하네스 변경 이력** 테이블에 기록한다.
