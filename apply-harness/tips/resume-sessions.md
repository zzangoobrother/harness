---
title: 이전 세션 이어가기
tags: [워크플로우, CLI]
updated: 2026-06-21
source: https://docs.claude.com/en/docs/claude-code/cli-reference
---

# 이전 세션 이어가기

## 상황
터미널을 닫았거나 다른 일을 하다 돌아왔을 때, 직전 대화 맥락을 처음부터 다시 설명하지 않고 그대로 이어가고 싶다.

## 방법
CLI 플래그로 세션을 복원한다.

- `claude --continue` (또는 `-c`) — 가장 최근 세션을 바로 이어간다.
- `claude --resume` (또는 `-r`) — 과거 세션 목록에서 골라 재개한다.

## 예시
```bash
# 가장 최근 대화를 이어서 시작
claude --continue

# 여러 과거 세션 중 선택해서 재개
claude --resume
```

## 주의점
- 이어가더라도 컨텍스트가 길면 토큰을 많이 쓴다. 새 작업이면 차라리 새 세션 + `/clear`가 가볍다. → [[context-management]]
- 세션 기록은 로컬에 저장된다. 다른 머신에서는 동일 세션을 이어갈 수 없다(확인 필요: 동기화 여부는 환경에 따라 다름).
- 정확한 플래그 별칭(`-c`, `-r`)과 동작은 버전에 따라 다를 수 있으니 `claude --help`로 확인한다.
