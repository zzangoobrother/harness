# Claude Code 사용 팁 인덱스

마지막 갱신: 2026-06-21

Claude Code를 실전에서 더 잘 쓰기 위한 팁 모음이다. 각 문서는 `상황 → 방법 → 예시 → 주의점` 구조로 작성돼 있다. 버전에 따라 달라질 수 있는 부분은 본문에 "확인 필요:"로 표시했다.

## 훅 (Hooks)
- [훅 이벤트 종류와 등록 구조](hooks-overview.md) — 9개 훅 이벤트와 settings.json 등록 기본 구조
- [PreToolUse 훅으로 위험한 명령 차단](hooks-pretooluse-block.md) — exit code 2로 위험 Bash 명령을 실행 전에 차단

## 설정 (Settings)
- [permissions로 권한 프롬프트 줄이기](settings-permissions.md) — allow/deny 규칙으로 반복 승인 제거
- [설정 파일 우선순위](settings-hierarchy.md) — 엔터프라이즈 > local > 프로젝트 > 사용자 적용 순서
- [env로 환경변수 주입하기](settings-env-vars.md) — 세션에만 적용되는 환경변수 정의

## 서브에이전트 (Subagents)
- [서브에이전트 개념과 정의 구조](subagents-overview.md) — `.claude/agents/*.md` 프론트매터와 시스템 프롬프트
- [무거운 작업을 서브에이전트에 위임하기](subagents-delegation.md) — 탐색·다중 파일 작업 병렬 위임으로 컨텍스트 절약
- [에이전트별 모델 지정으로 비용·속도 분기](subagents-model-routing.md) — sonnet/opus/haiku 작업별 분기

## 스킬 (Skills)
- [스킬 개념과 SKILL.md 구조](skills-overview.md) — description이 트리거 정확도를 좌우
- [스킬 점진적 공개로 토큰 아끼기](skills-progressive-disclosure.md) — SKILL.md는 짧게, 상세는 참조 파일로 분리

## 슬래시 커맨드 (Slash Commands)
- [커스텀 슬래시 커맨드 만들기](slash-commands-custom.md) — `.claude/commands/*.md`, $ARGUMENTS·셸 실행·파일 참조
- [자주 쓰는 내장 슬래시 커맨드](slash-commands-builtin.md) — /clear, /compact, /init, /config, /review

## MCP
- [MCP 개념과 서버 추가](mcp-overview.md) — `.mcp.json`으로 stdio/http 서버 연결
- [claude mcp add로 서버 등록하기](mcp-add-server.md) — CLI 등록과 local/project/user 스코프

## 워크플로우 / 입력
- [컨텍스트 관리로 토큰 아끼기](context-management.md) — /clear와 /compact를 상황별로 사용
- [자주 쓰는 단축키와 입력 팁](keyboard-shortcuts.md) — Esc 중단, Shift+Tab 모드 전환, 큐잉
- [플랜 모드로 실행 전 계획 검토받기](plan-mode.md) — Shift+Tab으로 변경 전 계획 승인
- [CLAUDE.md로 프로젝트 규약 관리하기](claude-md-memory.md) — 프로젝트/전역 메모리로 규약 자동 로드
- [이미지 붙여넣기로 스크린샷 기반 작업](images-and-paste.md) — 스크린샷·시안을 입력으로 활용
- [이전 세션 이어가기](resume-sessions.md) — `claude --continue` / `--resume`로 세션 복원
