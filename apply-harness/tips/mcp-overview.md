---
title: MCP 개념과 서버 추가
tags: [MCP]
updated: 2026-06-21
source: https://docs.claude.com/en/docs/claude-code/mcp
---

# MCP 개념과 서버 추가

## 상황
DB 조회, 이슈 트래커, 브라우저 자동화처럼 Claude Code 기본 도구 밖의 외부 시스템에 연결하고 싶을 때 MCP(Model Context Protocol) 서버를 붙인다. 서버가 제공하는 도구·리소스를 Claude가 그대로 호출할 수 있다.

## 방법
MCP 서버는 `.mcp.json`(프로젝트 공유) 또는 사용자 설정에 등록한다. 서버 연결 방식은 크게 세 가지다.

- `stdio` — 로컬 프로세스를 띄워 표준입출력으로 통신(가장 흔함)
- `sse` / `http` — 원격 HTTP 엔드포인트에 연결

## 예시
`.mcp.json` (stdio 서버):
```json
{
  "mcpServers": {
    "filesystem": {
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-filesystem", "/path/to/dir"]
    }
  }
}
```

원격 HTTP 서버:
```json
{
  "mcpServers": {
    "my-remote": {
      "type": "http",
      "url": "https://example.com/mcp"
    }
  }
}
```

## 주의점
- `.mcp.json`을 커밋하면 팀 전체가 같은 서버를 쓰지만, 토큰·비밀은 평문으로 넣지 말고 환경변수로 주입한다. → [[settings-env-vars]]
- 서버가 제공하는 도구는 권한 프롬프트 대상이다. 자주 쓰면 미리 허용해 둔다. → [[settings-permissions]]
- CLI로도 등록할 수 있다. → [[mcp-add-server]]
- 확인 필요: `type` 표기(`http`/`sse`)와 필드 이름은 버전에 따라 다를 수 있으니 공식 문서로 확인한다.
