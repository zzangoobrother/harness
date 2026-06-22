---
title: 서브에이전트 개념과 정의 구조
tags: [서브에이전트]
updated: 2026-06-21
source: https://docs.claude.com/en/docs/claude-code/sub-agents
---

# 서브에이전트 개념과 정의 구조

## 상황
큰 탐색이나 다중 파일 작업을 메인 대화에서 직접 하면 컨텍스트가 빠르게 찬다. 별도 컨텍스트를 가진 서브에이전트에 위임하면 결과만 받아 메인 대화를 가볍게 유지할 수 있다.

## 방법
서브에이전트는 `.claude/agents/<name>.md` 파일로 정의한다. 프론트매터에 메타데이터, 본문에 시스템 프롬프트를 쓴다.

- `name` — 에이전트 식별자
- `description` — 언제 이 에이전트를 쓰는지(자동 선택 기준)
- `tools` — 사용할 도구 목록(생략 시 기본 도구셋)
- `model` — 사용할 모델(sonnet/opus/haiku 등)

## 예시
```markdown
---
name: test-writer
description: 단위/통합 테스트 코드를 작성하는 에이전트. "테스트 짜줘" 요청에 사용.
tools: Read, Write, Edit, Glob, Grep, Bash
model: sonnet
---

당신은 테스트 작성 전문가입니다.
- 기존 테스트 컨벤션을 먼저 파악한다.
- 엣지 케이스를 포함해 작성한다.
- 작성 후 실행해 통과를 확인한다.
```

사용자/프로젝트 범위로 나눠 둘 수 있다: 개인용은 `~/.claude/agents/`, 프로젝트 공유용은 `<프로젝트>/.claude/agents/`.

## 주의점
- `description`이 구체적일수록 메인 에이전트가 알맞은 상황에 자동 위임한다. → [[skills-overview]]
- `tools`를 최소 권한으로 좁히면 안전하다(예: 탐색 에이전트엔 Write 제외).
- 서브에이전트는 독립 컨텍스트라 메인 대화 내용을 모른다. 필요한 맥락은 위임 프롬프트에 담아야 한다. → [[subagents-delegation]]
