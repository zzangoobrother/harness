---
title: 자주 쓰는 내장 슬래시 커맨드
tags: [슬래시커맨드]
updated: 2026-06-21
source: https://docs.claude.com/en/docs/claude-code/slash-commands
---

# 자주 쓰는 내장 슬래시 커맨드

## 상황
Claude Code에는 세션 관리·설정·리뷰용 내장 커맨드가 많다. 자주 쓰는 것들을 알아 두면 작업이 빨라진다.

## 방법
입력창에 `/`를 치면 사용 가능한 커맨드 목록이 뜬다. 대표적인 것들:

- `/clear` — 대화 컨텍스트를 초기화한다(새 작업 시작 시).
- `/compact` — 대화를 요약 압축해 토큰을 줄이고 맥락은 유지한다.
- `/init` — 코드베이스를 분석해 `CLAUDE.md`를 생성한다.
- `/config` — 모델·테마 등 설정을 연다.
- `/review` — 변경/PR을 리뷰한다.
- `/help` — 도움말.

## 예시
```text
# 새로운 작업으로 전환하며 이전 맥락 비우기
/clear

# 긴 세션에서 토큰이 차오를 때 압축
/compact

# 프로젝트에 CLAUDE.md가 없을 때 초기 생성
/init
```

## 주의점
- `/clear`는 맥락을 완전히 버린다. 진행 중 작업이 있으면 `/compact`가 안전하다. → [[context-management]]
- 사용 가능한 커맨드 목록과 동작은 버전·플랜에 따라 다르다. `/help` 또는 `/`로 현재 환경에서 확인한다.
- 커스텀 커맨드와 내장 커맨드는 같은 `/` 네임스페이스를 공유한다. → [[slash-commands-custom]]
