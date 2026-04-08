# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

web-wisecan-b2c is a B2C web application with a monorepo structure containing separate backend and frontend directories.

## Repository Structure

- `backend/` - Backend application (tech stack TBD)
- `frontend/` - Frontend application (tech stack TBD)

## Remote Repository

- GitHub: https://github.com/beanteacher/web-wisecan-b2c
- Default branch: `main`

## 주요 규칙

### Git 안전 규칙

- 절대 자동 push 하지 않음 — 반드시 사용자 확인 후 push
- API 키, 토큰, 비밀번호 등 민감 정보 커밋 금지
- 커밋 메시지는 한글로 작성 (팀 프로젝트 제외)
- push 전 git user.email 계정 확인 (hooks로 자동 검증)

### 코드 스타일

- 컨벤션 규칙: `.claude/rules/` 디렉토리 참고
- 프론트엔드: React/Next.js + FSD 아키텍처 (`.claude/rules/frontend-*.md`)
- 백엔드: Spring Boot + 레이어드 아키텍처 (`.claude/rules/backend-*.md`)

## 슬래시 커맨드

`.claude/commands/`에 정의된 커맨드를 `/커맨드명`으로 실행할 수 있습니다:

### 공통

- `/code-review` — PR 코드 리뷰
- `/pr` — Pull Request 생성

### 프론트엔드

- `/front-init-project` — Next.js + FSD 프로젝트 초기화
- `/front-new-feature` — FSD 기반 새 기능 개발
- `/front-refactor` — FSD 기반 체계적 리팩토링
- `/front-test` — 테스트 생성 및 실행

### 백엔드

- `/back-init-project` — Spring Boot 프로젝트 초기화
- `/back-new-feature` — 레이어드 아키텍처 기반 새 기능 개발
- `/back-refactor` — Spring 컨벤션 기반 체계적 리팩토링
- `/back-test` — 테스트 생성 및 실행
