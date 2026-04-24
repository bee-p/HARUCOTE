# COTE 프로젝트 작업 체크리스트

코딩테스트 문제풀이 사이트 + 데스크탑 펫 연동 프로젝트

## Phase 0. 프로젝트 기반 세팅

- [x] Spring Boot 프로젝트 생성
- [x] Gradle 빌드 환경 구성 (Java 23, Spring Boot 4.0.5)
- [x] PostgreSQL 연동 설정
- [x] SQL 쿼리 로깅 설정
- [x] 도메인별 패키지 구조 생성 (user / problem / submission / challenge / pet / item / common)
- [ ] `.gitignore` 점검 (IDE, 빌드 산출물, 환경변수)
- [ ] Git 저장소 초기화 및 첫 커밋
- [ ] `common/exception/GlobalExceptionHandler` 작성
- [ ] `common/dto/ApiResponse` 공통 응답 포맷 정의
- [ ] `application-local.yml` / `application-prod.yml` 프로파일 분리
- [ ] 민감정보(DB 비밀번호 등) 환경변수로 분리

---

## Phase 1. User 도메인 (인증/인가)

- [ ] `User` 엔티티 설계 (id, email, nickname, 가입일 등)
- [ ] `UserRepository` 작성
- [ ] Spring Security 기본 설정
- [ ] OAuth2 로그인 연동 (GitHub 또는 Google)
- [ ] JWT 발급/검증 로직
- [ ] 회원가입 / 로그인 / 내 정보 조회 API
- [ ] 인증 필터 테스트

---

## Phase 2. Problem 도메인

### 2-1. 엔티티 & API

- [ ] `Problem` 엔티티 설계 (title, statement, difficulty, tags, timeLimit, memoryLimit, source, externalId 등)
- [ ] `ProblemExample` 엔티티 (input, output, explanation)
- [ ] `Tag` 엔티티 또는 enum 정의 (dp, greedy, graph 등)
- [ ] 문제 조회 / 랜덤 조회 API
- [ ] 카테고리(태그)/난이도별 필터링 조회
- [ ] 페이징 처리

### 2-2. LeetCode 문제 수집 배치

- [ ] LeetCode 비공식 GraphQL API 연동 (`https://leetcode.com/graphql`)
- [ ] 전체 문제 목록 수집 (`problemsetQuestionList` 쿼리)
- [ ] 문제 상세 수집 (`question(titleSlug)` 쿼리 — 지문, 예제, 태그, 난이도)
- [ ] HTML 지문 파싱 및 정제 (Jsoup)
- [ ] 응답 → `Problem` 엔티티 변환 매퍼 작성
- [ ] 중복 수집 방지 (`externalId = titleSlug` 기준 upsert)
- [ ] Rate limit 준수 (요청 간 딜레이 설정)
- [ ] `@Scheduled` 배치 작성 (주 1회 신규 문제 수집)
- [ ] 수집 로그 기록
- [ ] 초기 전체 문제 수동 트리거 API (`POST /admin/problems/sync`)

### 2-3. AI 문제 생성 (추후)

- [ ] LeetCode 문제 + 솔루션 코드 데이터셋 구성
- [ ] 모델 선정 및 파인튜닝 (별도 Python 레포)
- [ ] 생성 문제 검증 파이프라인 (파싱 → 코드 실행 → 예제 일관성 확인)
- [ ] 생성 문제를 DB에 저장하는 API 연동

---

## Phase 3. Submission 도메인 (풀이 제출)

- [ ] `Submission` 엔티티 (userId, problemId, 코드, 언어, 결과, 소요시간, 제출일)
- [ ] 풀이 제출 API
- [ ] 채점 방식 결정 (간단한 문자열 비교 vs 실제 실행)
- [ ] 채점 결과 저장
- [ ] 내 제출 이력 조회
- [ ] 문제별 성공률 통계
- [ ] **풀이 성공 시 `ProblemSolvedEvent` 발행**

---

## Phase 4. Challenge 도메인 (도전과제)

- [ ] `Challenge` 엔티티 (이름, 조건, 보상 아이템 ID)
- [ ] 도전과제 타입 정의 (카테고리별 N개 풀이 / 제한시간 내 풀이 / 연속 풀이 등)
- [ ] `UserChallengeProgress` 엔티티 (진행도 추적)
- [ ] `ProblemSolvedEvent` 리스너 → 진행도 업데이트
- [ ] 도전과제 달성 시 `ChallengeCompletedEvent` 발행
- [ ] 내 도전과제 진행 현황 조회 API
- [ ] 전체 도전과제 목록 조회 API

---

## Phase 5. Pet 도메인

- [ ] `Pet` 엔티티 (userId, 현재 착용 아이템, 상태 등)
- [ ] 펫 초기 생성 (회원가입 시 기본 펫 지급)
- [ ] 펫 상태 조회 API (데스크탑 앱이 호출)
- [ ] 아이템 착용/해제 API

---

## Phase 6. Item 도메인 (의상/아이템)

- [ ] `Item` 엔티티 (이름, 타입, 썸네일, 획득 조건)
- [ ] `UserItem` 엔티티 (해금 여부, 획득일)
- [ ] `ChallengeCompletedEvent` 리스너 → 아이템 해금 처리
- [ ] 내 보유 아이템 조회 API
- [ ] 전체 아이템 목록 조회 API (잠김/해금 표시)

---

## Phase 7. 데스크탑 펫 연동

- [ ] 데스크탑 앱 기술 선정 (Electron / Tauri / Unity 등)
- [ ] 데스크탑 앱 ↔ 서버 인증 방식 (API 토큰 등)
- [ ] 펫 상태 동기화 API 정의
- [ ] 해금 알림 전달 방식 결정 (Polling / WebSocket / SSE)
- [ ] 데스크탑 앱 프로토타입

---

## Phase 8. 프론트엔드 (웹)

- [ ] 프론트엔드 기술 선정 (React / Vue / Thymeleaf 등)
- [ ] 문제 목록 / 상세 페이지
- [ ] 코드 에디터 (Monaco Editor 등)
- [ ] 제출 결과 표시
- [ ] 내 도전과제 진행 상황 화면
- [ ] 내 아이템 / 펫 미리보기 화면

---

## Phase 9. 테스트 & 품질

- [ ] 각 도메인 Service 단위 테스트
- [ ] Repository 테스트 (`@DataJpaTest`)
- [ ] Controller 통합 테스트 (`@SpringBootTest`)
- [ ] 이벤트 체인 통합 테스트 (문제풀이 → 도전과제 → 아이템 해금)
- [ ] Security 관련 테스트

---

## Phase 10. 배포 / 마무리

- [ ] Dockerfile 작성
- [ ] docker-compose (앱 + PostgreSQL)
- [ ] 배포 환경 결정 (로컬 / 라즈베리파이 / 클라우드)
- [ ] README 작성 (스크린샷, 실행 방법)

---

## 참고: 진행 규칙

- 각 Phase는 순차적이지 않아도 됨. 의존성이 있는 것만 지키면 됨
  - 예: Challenge는 Submission이 먼저 있어야 하고, Item은 Challenge가 있어야 함
- 새 기능 구현 시 이 파일을 업데이트
- 체크박스는 구현 + 최소한의 테스트까지 완료된 경우에만 체크
