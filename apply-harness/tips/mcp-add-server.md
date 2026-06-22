---
title: claude mcp add로 서버 등록하기
tags: [MCP, CLI]
updated: 2026-06-21
source: https://docs.claude.com/en/docs/claude-code/mcp
---

# claude mcp add로 서버 등록하기

## 상황
`.mcp.json`을 직접 편집하는 대신 CLI로 MCP 서버를 빠르게 등록하고, 적용 범위(스코프)를 지정하고 싶을 때 `claude mcp` 명령을 쓴다.

## 방법
터미널에서 `claude mcp add`로 서버를 추가한다. 스코프 옵션으로 적용 범위를 정한다.

- `local` — 현재 사용자·현재 프로젝트에만(개인용, 공유 안 됨)
- `project` — 프로젝트의 `.mcp.json`에 기록(팀 공유, 커밋 대상)
- `user` — 사용자 전역(모든 프로젝트)

관련 명령:
- `claude mcp list` — 등록된 서버 목록
- `claude mcp get <name>` — 특정 서버 정보
- `claude mcp remove <name>` — 제거

## 예시
```bash
# 로컬 stdio 서버 추가
claude mcp add filesystem -- npx -y @modelcontextprotocol/server-filesystem /path

# 프로젝트 스코프로 추가 (.mcp.json에 기록)
claude mcp add --scope project github -- npx -y @modelcontextprotocol/server-github

# 등록 확인
claude mcp list
```

## 주의점
- `--`(이중 대시) 뒤는 서버 실행 명령으로 그대로 전달된다. 옵션과 명령을 구분하는 경계다.
- `project` 스코프로 넣으면 `.mcp.json`이 변경되어 커밋된다. 비밀값은 환경변수로 분리한다. → [[settings-env-vars]]
- 확인 필요: 스코프 플래그 표기(`--scope` 값, 기본 스코프)와 인증 토큰 전달 방식은 버전에 따라 다를 수 있으니 `claude mcp add --help`로 확인한다.
