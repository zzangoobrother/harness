---
title: CLAUDE.md로 프로젝트 규약 관리하기
tags: [워크플로우, 메모리, 설정]
updated: 2026-06-21
source: https://docs.claude.com/en/docs/claude-code/memory
---

# CLAUDE.md로 프로젝트 규약 관리하기

## 상황
"커밋 메시지는 한국어로", "이 디렉터리는 건드리지 마", "테스트는 항상 작성" 같은 규약을 매번 설명하기 번거롭다. `CLAUDE.md`에 적어 두면 세션마다 자동으로 컨텍스트에 로드된다.

## 방법
- **프로젝트 규약** → 프로젝트 루트의 `CLAUDE.md` 또는 `.claude/CLAUDE.md`. 팀과 공유(커밋)한다.
- **개인 전역 규약** → `~/.claude/CLAUDE.md`. 모든 프로젝트에 적용된다.
- `/init`로 코드베이스를 분석해 초기 `CLAUDE.md`를 생성할 수 있다.

규약은 명령형으로 간결하게 쓴다. 코딩 컨벤션, 빌드·테스트 명령, 금지 사항, 디렉터리 설명 등을 담는다.

## 예시
`CLAUDE.md`:
```markdown
## 커뮤니케이션
- 응답·주석·커밋 메시지는 한국어로 작성한다.

## 빌드/테스트
- 테스트: `npm test`
- 린트: `npm run lint`

## 규칙
- `src/legacy/`는 수정하지 않는다.
- 기능 추가 시 단위 테스트를 함께 작성한다.
```

## 주의점
- CLAUDE.md가 길어지면 매 세션 토큰을 차지한다. 핵심만 남기고 상세는 참조 문서로 분리한다. → [[skills-progressive-disclosure]]
- 사용자 직접 지시가 CLAUDE.md보다 우선한다. 규약은 기본값일 뿐 절대 규칙은 아니다.
- 자주 쓰는 절차는 CLAUDE.md 대신 스킬로 빼면 필요할 때만 로드되어 효율적이다. → [[skills-overview]]
