---
title: PreToolUse 훅으로 위험한 명령 차단
tags: [훅, 보안]
updated: 2026-06-21
source: https://docs.claude.com/en/docs/claude-code/hooks
---

# PreToolUse 훅으로 위험한 명령 차단

## 상황
`rm -rf`, 강제 푸시, 비밀파일 접근처럼 위험한 Bash 명령이 실행되기 전에 막고 싶을 때 `PreToolUse` 훅을 쓴다.

## 방법
`PreToolUse` 훅은 도구 실행 직전에 호출된다. 훅 스크립트가 도구 입력(JSON)을 stdin으로 받고, **차단하려면 exit code 2를 반환**하거나 `permissionDecision: "deny"`가 담긴 JSON을 stdout으로 출력한다.

1. `matcher`를 `Bash`로 지정한다.
2. 스크립트에서 명령 문자열을 검사한다.
3. 위험 패턴이면 비정상 종료(또는 deny JSON)로 차단한다.

## 예시
```json
{
  "hooks": {
    "PreToolUse": [
      {
        "matcher": "Bash",
        "hooks": [
          { "type": "command", "command": "$CLAUDE_PROJECT_DIR/.claude/hooks/guard.sh" }
        ]
      }
    ]
  }
}
```

```bash
#!/usr/bin/env bash
# guard.sh — stdin으로 도구 입력 JSON을 받는다
input=$(cat)
cmd=$(echo "$input" | jq -r '.tool_input.command // empty')
if echo "$cmd" | grep -Eq 'rm -rf /|git push --force'; then
  echo "위험한 명령이 차단되었습니다: $cmd" >&2
  exit 2   # exit 2 → 도구 실행 차단
fi
exit 0
```

## 주의점
- exit code 0 = 허용, exit code 2 = 차단으로 동작한다. 그 외 코드/출력 규약은 공식 문서로 확인한다.
- 정규식이 너무 느슨하면 정상 명령까지 막힌다. 패턴을 좁게 잡는다.
- 확인 필요: stdin JSON의 정확한 필드 경로(`tool_input.command` 등)는 버전에 따라 다를 수 있다.
