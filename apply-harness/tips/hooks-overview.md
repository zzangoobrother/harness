---
title: 훅 이벤트 종류와 등록 구조
tags: [훅, 설정]
updated: 2026-06-21
source: https://docs.claude.com/en/docs/claude-code/hooks
---

# 훅 이벤트 종류와 등록 구조

## 상황
도구 실행 전후나 세션 시작·종료 같은 특정 시점에 자동으로 스크립트를 실행하고 싶을 때 훅을 쓴다. 코드 포맷팅, 위험 명령 차단, 로깅 등을 손대지 않고 자동화할 수 있다.

## 방법
훅은 `settings.json`의 `hooks` 키 아래에 이벤트별로 등록한다. 주요 이벤트는 다음과 같다.

- `PreToolUse` — 도구 실행 직전 (차단/검증에 사용)
- `PostToolUse` — 도구 실행 직후 (포맷팅/로깅에 사용)
- `UserPromptSubmit` — 사용자 입력 제출 시
- `Stop` — Claude가 응답을 마칠 때
- `SubagentStop` — 서브에이전트가 끝날 때
- `SessionStart` / `SessionEnd` — 세션 시작·종료
- `PreCompact` — 컨텍스트 압축 직전
- `Notification` — 알림 발생 시

각 훅은 `matcher`(도구 이름 패턴)와 실행할 `command`로 구성한다.

## 예시
```json
{
  "hooks": {
    "PostToolUse": [
      {
        "matcher": "Edit|Write",
        "hooks": [
          {
            "type": "command",
            "command": "prettier --write \"$CLAUDE_FILE_PATHS\""
          }
        ]
      }
    ]
  }
}
```

## 주의점
- `matcher`는 도구 이름에 대한 정규식이다. 모든 도구를 잡으려면 `"*"` 또는 빈 문자열을 쓴다.
- 훅 명령은 셸에서 실행되므로, 신뢰할 수 없는 입력을 그대로 명령에 넣지 않는다.
- 확인 필요: 훅에 전달되는 환경변수/입력 JSON 필드 이름은 버전에 따라 다를 수 있으니 공식 문서로 확인한다.
