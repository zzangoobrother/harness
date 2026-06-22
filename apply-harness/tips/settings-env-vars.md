---
title: env로 환경변수 주입하기
tags: [설정, 환경변수]
updated: 2026-06-21
source: https://docs.claude.com/en/docs/claude-code/settings
---

# env로 환경변수 주입하기

## 상황
Claude Code가 실행하는 명령·세션에 일관된 환경변수(API 키 경로, 빌드 플래그, 동작 토글 등)를 주입하고 싶을 때 `settings.json`의 `env`를 쓴다.

## 방법
`settings.json`에 `env` 객체를 두고 키-값을 정의한다. 여기 정의된 값은 세션 환경에 적용된다.

## 예시
```json
{
  "env": {
    "NODE_ENV": "development",
    "DISABLE_TELEMETRY": "1"
  }
}
```

## 주의점
- 비밀값(실제 API 키 등)은 settings.json에 평문으로 두지 말고, 셸 환경이나 비밀관리 도구를 통해 주입한다. 특히 커밋되는 `.claude/settings.json`에는 넣지 않는다.
- 개인 전용 값은 `.claude/settings.local.json`에 둔다. → [[settings-hierarchy]]
- 확인 필요: Claude Code 동작 자체를 바꾸는 특수 환경변수(예: 텔레메트리/모델 관련) 이름은 버전에 따라 다를 수 있으니 공식 문서로 확인한다.
