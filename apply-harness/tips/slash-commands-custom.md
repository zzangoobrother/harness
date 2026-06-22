---
title: 커스텀 슬래시 커맨드 만들기
tags: [슬래시커맨드]
updated: 2026-06-21
source: https://docs.claude.com/en/docs/claude-code/slash-commands
---

# 커스텀 슬래시 커맨드 만들기

## 상황
"이 형식으로 PR 설명 써줘", "변경 파일 린트하고 요약해줘"처럼 반복하는 프롬프트가 있다면 슬래시 커맨드로 저장해 `/이름`으로 호출한다.

## 방법
`.claude/commands/<name>.md` 파일을 만든다. 파일 내용이 곧 프롬프트가 되고, `/name`으로 실행된다.

- `$ARGUMENTS` — 호출 시 뒤에 붙인 인자가 치환된다.
- `!`백틱 명령`` — 커맨드 실행 시 셸 명령을 미리 돌려 그 출력을 프롬프트에 넣는다.
- `@파일경로` — 파일 내용을 프롬프트에 포함한다.
- 프론트매터로 `description`, 허용 도구 등을 지정할 수 있다.

## 예시
`.claude/commands/fix-issue.md`:
```markdown
---
description: 깃허브 이슈 번호를 받아 수정한다
---

다음 이슈를 해결한다: #$ARGUMENTS

현재 변경 상태:
!`git status --short`

관련 가이드라인:
@docs/contributing.md
```

호출:
```text
/fix-issue 123
```

개인용은 `~/.claude/commands/`, 프로젝트 공유용은 `<프로젝트>/.claude/commands/`에 둔다. 하위 폴더로 네임스페이스를 만들 수 있다(예: `commands/git/sync.md` → `/git:sync`로 호출되는 식 — 확인 필요).

## 주의점
- `!`백틱`` 셸 실행은 커맨드 작성자가 신뢰하는 명령만 넣는다.
- 인자가 여러 개일 때 `$ARGUMENTS`는 전체 문자열로 치환된다. 개별 인자 분리 표기는 버전에 따라 다를 수 있으니 확인한다.
- 내장 커맨드와 이름이 겹치지 않게 한다. → [[slash-commands-builtin]]
