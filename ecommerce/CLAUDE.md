# ecommerce

React(프론트) + Spring Boot(백엔드) 기반 이커머스 MVP 프로젝트.

## 언어 규약
- 문서/주석/커밋 메시지: 한국어
- 코드/파일명/식별자: 영어

## 하네스: 이커머스 빌드

**목표:** 상품·장바구니·회원가입/로그인·주문/모의결제로 구성된 이커머스 MVP를 에이전트 팀(계약설계→병렬구현→경계면검증)으로 구축·확장한다.

**트리거:** 이커머스 기능 구축·수정·확장(상품/장바구니/인증/주문/결제 등) 요청 시 `ecommerce-build` 스킬을 사용하라. 단순 단일 파일 질문·설명은 직접 응답 가능.

**구성:** 에이전트 4 (`api-architect`, `backend-engineer`, `frontend-engineer`, `qa-integrator`) + 스킬 5 (`ecommerce-api-design`, `spring-boot-ecommerce`, `react-ecommerce-ui`, `integration-qa`, `ecommerce-build`). 상세는 `.claude/agents/`, `.claude/skills/` 및 오케스트레이터 스킬 참조.

**변경 이력:**
| 날짜 | 변경 내용 | 대상 | 사유 |
|------|----------|------|------|
| 2026-07-19 | 초기 구성 (에이전트 4 + 스킬 5) | 전체 | - |
