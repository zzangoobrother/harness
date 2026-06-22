---
title: permissions로 권한 프롬프트 줄이기
tags: [설정, 권한]
updated: 2026-06-21
source: https://docs.claude.com/en/docs/claude-code/settings
---

# permissions로 권한 프롬프트 줄이기

## 상황
매번 같은 명령에 대해 권한 승인 프롬프트가 떠서 번거로울 때, 자주 쓰는 안전한 도구·명령을 미리 허용(allow)하고 위험한 것은 차단(deny)해 둔다.

## 방법
`settings.json`의 `permissions` 객체에 `allow`, `deny`, `ask` 규칙을 배열로 등록한다. 규칙은 `도구명(인자패턴)` 형식이다.

- `allow` — 프롬프트 없이 자동 허용
- `deny` — 무조건 차단
- `ask` — 매번 물어봄

## 예시
```json
{
  "permissions": {
    "allow": [
      "Bash(npm run test:*)",
      "Bash(git status)",
      "Read(~/.zshrc)"
    ],
    "deny": [
      "Bash(rm -rf:*)",
      "Read(./.env)"
    ]
  }
}
```

## 주의점
- 인자 패턴의 `:*`는 해당 접두사로 시작하는 모든 명령을 의미한다.
- `deny`가 `allow`보다 우선한다.
- 프로젝트 단위로 공유하고 싶으면 `.claude/settings.json`, 개인용은 `.claude/settings.local.json`(보통 gitignore 대상)에 둔다. → [[settings-hierarchy]]
- `/permissions` 또는 권한 프롬프트의 "Allow for this session"으로도 즉석에서 추가할 수 있다.
