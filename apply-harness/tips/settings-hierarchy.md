---
title: 설정 파일 우선순위
tags: [설정]
updated: 2026-06-21
source: https://docs.claude.com/en/docs/claude-code/settings
---

# 설정 파일 우선순위

## 상황
같은 설정이 여러 곳에 있을 때 어떤 값이 적용되는지 헷갈릴 때가 있다. 우선순위를 알아야 팀 공유 설정과 개인 설정을 의도대로 나눌 수 있다.

## 방법
설정은 범위가 좁을수록(더 로컬일수록) 우선한다. 대략 다음 순서로 덮어쓴다(위가 더 강함).

1. 엔터프라이즈 관리 정책 (조직 차원, 변경 불가)
2. 프로젝트 로컬 — `.claude/settings.local.json` (개인용, 보통 gitignore)
3. 프로젝트 공유 — `.claude/settings.json` (팀 공유, 커밋 대상)
4. 사용자 전역 — `~/.claude/settings.json`

## 예시
```text
~/.claude/settings.json            # 내 모든 프로젝트 기본값
프로젝트/.claude/settings.json      # 이 팀이 공유하는 설정 (git 커밋)
프로젝트/.claude/settings.local.json # 나만의 오버라이드 (git 무시)
```

같은 키가 충돌하면 더 위(로컬) 파일 값이 이긴다.

## 주의점
- `.claude/settings.local.json`은 보통 `.gitignore`에 넣어 개인 권한·환경값이 공유되지 않게 한다.
- 팀 공통 규약(허용 명령, 훅)은 `.claude/settings.json`에 두고 커밋한다. → [[settings-permissions]]
- 확인 필요: 엔터프라이즈 정책 파일의 정확한 경로는 OS·배포 방식에 따라 다르니 공식 문서로 확인한다.
