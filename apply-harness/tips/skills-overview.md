---
title: 스킬 개념과 SKILL.md 구조
tags: [스킬]
updated: 2026-06-21
source: https://docs.claude.com/en/docs/claude-code/skills
---

# 스킬 개념과 SKILL.md 구조

## 상황
특정 작업을 할 때마다 따라야 하는 절차·규약·도메인 지식이 있다면, 매번 설명하는 대신 스킬로 만들어 둔다. 관련 작업이 들어오면 Claude가 알아서 해당 스킬을 불러 따른다.

## 방법
스킬은 `.claude/skills/<name>/SKILL.md` 파일로 정의한다. 프론트매터에 `name`과 `description`을 쓰고, 본문에 절차·규약을 적는다.

- `name` — 스킬 식별자
- `description` — **언제 이 스킬을 써야 하는지**. 트리거 정확도를 좌우하는 가장 중요한 필드.

## 예시
```markdown
---
name: tip-format
description: tips/ 하위 팁 문서의 표준 포맷과 프론트매터 규약. 새 팁 문서 작성 시 반드시 따른다.
---

# tip-format

## 파일 위치/이름
- 경로: tips/<kebab-case>.md

## 본문 구조
상황 → 방법 → 예시 → 주의점
```

디렉터리 구조:
```text
.claude/skills/
  tip-format/
    SKILL.md
  tip-publish/
    SKILL.md
```

## 주의점
- `description`은 "무엇을 하는지"보다 "언제 발동하는지"를 구체적으로 써야 Claude가 알맞게 호출한다. 모호하면 트리거되지 않는다.
- SKILL.md 본문이 길어지면 핵심만 남기고 상세는 참조 파일로 분리한다. → [[skills-progressive-disclosure]]
- 개인용은 `~/.claude/skills/`, 프로젝트 공유용은 `<프로젝트>/.claude/skills/`에 둔다.
