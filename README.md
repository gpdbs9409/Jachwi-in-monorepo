# Jachwi-in Monorepo

자취인(Jachwi-in) 프로젝트의 통합 모노레포입니다. 기존에 서비스별로 분리되어 있던 저장소들을
각자의 커밋 히스토리를 보존한 채 `git subtree`로 합쳤습니다.

## 서비스 구성

- `Jachwi_in-Web-React` — 프론트엔드 (React)
- `Jachwi_in-Server-Spring` — 메인 백엔드 (Spring Boot)
- `Jachwi_in-Auth-Server` — 인증 서버 (Spring Boot)
- `Jachwi_in-FastAPI` — 임베딩/벡터 검색 서비스 (FastAPI + Qdrant)
- `Jachwi_in-Embedding-Benchmark` — 임베딩 모델 벤치마크
- `jachwiin-scripts` — 운영/데이터 스크립트

각 서브디렉터리는 독립 저장소로 계속 운영할 수도 있고, 이 모노레포에서 바로 개발을 이어갈 수도
있습니다. 각 서비스의 원래 커밋 로그는 아래처럼 그대로 조회할 수 있습니다:

```bash
git log -- Jachwi_in-Server-Spring
```

## 참고

`CLAUDE.md`, `architecture.html`은 프로젝트 전체 문서입니다.
