---
title: 스킬 점진적 공개로 토큰 아끼기
tags: [스킬, 생산성]
updated: 2026-06-21
source: https://docs.claude.com/en/docs/claude-code/skills
---

# 스킬 점진적 공개로 토큰 아끼기

## 상황
스킬에 모든 세부 절차·예시를 다 적으면 SKILL.md가 길어지고, 발동될 때마다 그 전부가 컨텍스트에 올라간다. 정작 자주 필요한 건 핵심 절차뿐이다.

## 방법
점진적 공개(progressive disclosure) 원칙을 적용한다.

1. **SKILL.md는 짧게** — 언제 쓰는지(description), 핵심 절차, 진입점만 둔다.
2. **상세 내용은 별도 파일로 분리** — 긴 예시, 레퍼런스 표, 엣지 케이스는 같은 폴더의 다른 `.md`로 빼고 SKILL.md에서 가리킨다.
3. Claude는 먼저 SKILL.md를 읽고, 필요할 때만 참조 파일을 연다.

## 예시
```text
.claude/skills/api-guidelines/
  SKILL.md              # 짧은 진입점 + 절차 요약
  references/
    error-codes.md      # 상세 에러 코드 표 (필요할 때만 로드)
    examples.md         # 긴 코드 예시
```

```markdown
---
name: api-guidelines
description: 내부 API 호출 규약. API 클라이언트 작성/수정 시 사용.
---

# api-guidelines

## 핵심 절차
1. 인증 헤더 부착
2. 에러 코드 처리 — 상세는 references/error-codes.md 참고
3. 재시도 정책 적용

자세한 예시는 references/examples.md 참고.
```

## 주의점
- 메인 SKILL.md에 "필요하면 X 파일을 읽으라"고 명시해야 Claude가 참조 파일을 연다.
- 너무 잘게 쪼개면 오히려 파일 탐색 비용이 든다. 핵심/상세 2단계 정도가 적당하다.
- 같은 원칙은 CLAUDE.md에도 적용된다. 길어지면 참조 문서로 분리한다. → [[claude-md-memory]]
