# CLAUDE.md — 헬스장 관리 웹 프로젝트 개발 가이드라인

> 이 문서는 Claude(및 모든 기여자)가 이 프로젝트에서 코드를 작성/수정할 때 반드시 따라야 하는 규칙이다.
> **핵심 원칙: 한 번에 모든 것을 만들지 않는다. 아주 작은 단위로 쪼개서, 파일 단위로 검증하며 전진한다.**

---

## 0. 프로젝트 개요

헬스장(피트니스 센터) 운영을 위한 관리자용 웹 애플리케이션. 관리자/데스크 직원이 회원, 출석, 결제, 이용권, PT권, PT 예약을 통합 관리한다.

### 핵심 기능 범위 (도메인)
1. **계정정보관리** — 관리자 로그인(인증/인가), 회원 가입 및 정보 CRUD
2. **출석관리** — 회원 번호 입력 기반 출석 체크 및 현황 조회
3. **금액결제** — 결제 내역 기록 및 관리
4. **헬스장 이용권 관리** — 개월 수 기준 이용권 생성 및 회원 부여
5. **PT권 관리** — PT 횟수 부여 및 잔여 횟수 차감
6. **PT 예약시간 관리** — 트레이너 스케줄 및 회원 예약 매칭

---

## 1. 기술 스택

| 레이어 | 기술 |
| --- | --- |
| Frontend | React 18 (Vite), TypeScript, Tailwind CSS |
| 상태/서버통신 | TanStack Query (서버 상태), Zustand (전역 클라이언트 상태), Axios |
| 라우팅 | React Router |
| 폼/검증 | React Hook Form + Zod |
| Backend | Spring Boot 4.x, Java 17+, Spring Web(MVC), Spring Security, Spring Data JPA |
| DB | PostgreSQL 15+ |
| 인증 | JWT (Access + Refresh) |
| 빌드/도구 | ESLint, Prettier (FE) / Gradle, Checkstyle (BE) |

> 스택은 확정본이다. 임의로 다른 라이브러리를 도입하지 말 것. 필요하면 먼저 제안하고 승인받는다.

---

## 2. 개발 진행 방식 (가장 중요 — 반드시 준수)

> **절대 규칙: 한 번에 전체 기능/여러 파일을 쏟아내지 않는다.**

1. **MVP 단위로 쪼갠다.** 하나의 기능을 "가장 작게 동작하는 조각"으로 분해한다.
   - 예) "회원 가입"은 → ①엔티티 → ②Repository → ③Service(생성 1건) → ④Controller(POST 1개) → ⑤FE API 함수 → ⑥폼 UI → ⑦검증/에러 처리 순으로 나눈다.
2. **한 번에 파일 1개(많아야 2~3개)씩** 작성/수정한다.
3. **각 조각마다 멈추고 검증한다.** 코드를 낸 뒤:
   - 무엇을 만들었는지, 어디에 있는지, 어떻게 확인하는지(빌드/테스트/실행 방법)를 한두 줄로 요약한다.
   - 다음에 무엇을 할지 제안하고 **사용자 확인을 기다린다.**
4. **작업 시작 전 계획을 먼저 보여준다.** 여러 파일이 얽히는 작업이면, 파일 목록과 순서를 먼저 제시하고 동의를 얻은 뒤 첫 파일만 작성한다.
5. **추측으로 채우지 않는다.** 요구사항이 모호하면 코드를 지어내지 말고 질문한다.
6. **기존 코드를 존중한다.** 이미 있는 패턴/네이밍/구조를 먼저 읽고, 그것과 일관되게 작성한다.
7. **범위를 넘지 않는다.** 요청받지 않은 리팩터링/파일 생성/기능 추가를 임의로 하지 않는다.

이 절차를 어기고 대량의 코드를 한 번에 생성하는 것은 이 프로젝트에서 **규칙 위반**이다.

---

## 3. 디렉토리 및 아키텍처 구조

### 3.1 저장소 레이아웃 (모노레포)
```
gym_project/
├── CLAUDE.md
├── frontend/          # React (Vite)
└── backend/           # Spring Boot
```

### 3.2 Frontend 구조 (기능 중심 / feature-based)
```
frontend/src/
├── app/                 # 앱 진입점, 라우터, 전역 프로바이더
├── features/            # 도메인별 기능 모듈 (핵심)
│   ├── auth/
│   ├── members/
│   ├── attendance/
│   ├── payments/
│   ├── memberships/     # 헬스 이용권
│   ├── pt-passes/       # PT권
│   └── pt-reservations/ # PT 예약
│       ├── api/         # 이 기능의 서버 통신 함수
│       ├── components/  # 이 기능 전용 컴포넌트
│       ├── hooks/       # useXxxQuery, useXxxMutation 등
│       ├── types/       # 이 기능의 타입
│       └── pages/       # 라우팅되는 페이지 컴포넌트
├── components/          # 공용 UI 컴포넌트 (Button, Modal, Table...)
├── hooks/               # 공용 훅
├── lib/                 # axios 인스턴스, 유틸, 상수
├── types/               # 전역 공용 타입
└── styles/              # Tailwind 전역 스타일
```
- **규칙:** 특정 도메인에만 쓰이면 `features/<domain>` 안에, 2개 이상 도메인이 공유하면 최상위 공용 폴더로 올린다.

### 3.3 Backend 구조 (계층형 + 도메인 패키지)
```
backend/src/main/java/com/gym/
├── GymApplication.java
├── common/              # 공통: 예외, 응답 래퍼, 설정, 유틸
│   ├── config/
│   ├── exception/
│   └── response/        # ApiResponse<T>, ErrorResponse
├── security/            # JWT, 인증/인가 필터
└── domain/
    ├── member/
    │   ├── controller/  # REST 엔드포인트 (얇게)
    │   ├── service/     # 비즈니스 로직
    │   ├── repository/  # Spring Data JPA
    │   ├── entity/      # JPA 엔티티
    │   └── dto/         # request/response DTO
    ├── attendance/
    ├── payment/
    ├── membership/
    ├── ptpass/
    └── ptreservation/
```
- **계층 의존 방향:** `Controller → Service → Repository`. 역방향 금지.
- **엔티티는 컨트롤러/외부로 노출하지 않는다.** 반드시 DTO로 변환해서 주고받는다.
- **비즈니스 로직은 Service에만.** Controller는 요청 검증 + 위임만, Repository는 데이터 접근만.

---

## 4. 코드 스타일 가이드 (Clean Code)

### 4.1 공통 원칙
- **의도가 드러나는 이름**을 쓴다. 축약/모호한 이름 금지 (`d`, `tmp`, `data2` ❌).
- **함수는 한 가지 일만** 한다. 길어지면 (대략 30~40줄 초과) 쪼갠다.
- **매직 넘버/문자열 금지.** 상수로 추출한다. (예: `MAX_PT_COUNT`)
- **깊은 중첩보다 조기 반환**(early return / guard clause)을 선호한다.
- 주석은 "무엇"이 아니라 **"왜"**를 설명한다. 코드로 드러나는 건 주석 달지 않는다.
- 죽은 코드/주석 처리된 코드/`console.log`·`System.out.println` 디버그 잔재를 남기지 않는다.

### 4.2 네이밍 규칙

| 대상 | 규칙 | 예시 |
| --- | --- | --- |
| 변수/함수 (TS/Java) | camelCase | `remainingCount`, `getMemberById` |
| React 컴포넌트/파일 | PascalCase | `MemberList.tsx`, `AttendanceForm` |
| 커스텀 훅 | `use` 접두사 | `useMemberQuery` |
| 상수 | UPPER_SNAKE_CASE | `MAX_PT_COUNT`, `TOKEN_KEY` |
| 타입/인터페이스 (TS) | PascalCase, `I`/`T` 접두사 없이 | `Member`, `PaymentRequest` |
| Java 클래스 | PascalCase | `MemberService`, `PaymentEntity` |
| Boolean | `is`/`has`/`can` 접두사 | `isActive`, `hasActivePass` |
| 이벤트 핸들러 (FE) | `handle` 접두사 | `handleSubmit`, `handleCheckIn` |
| DB 테이블/컬럼 | snake_case | `pt_pass`, `remaining_count` |
| REST 경로 | 복수형 명사, kebab/소문자 | `/api/members`, `/api/pt-reservations` |

### 4.3 함수/API 네이밍 컨벤션 (Backend)
- 조회 `find`/`get`, 생성 `create`, 수정 `update`, 삭제 `delete`.
- 단건 없으면 예외 던지는 건 `getXxx`, Optional 반환은 `findXxx`로 구분.

### 4.4 포매팅
- FE: **Prettier + ESLint** 설정을 따른다. 저장 시 자동 포맷.
- BE: 구글 자바 스타일 기준. 들여쓰기 4 spaces.
- 커밋 전 린트/포맷 통과가 기본. 포맷 이슈로 리뷰 시간 낭비하지 않는다.

---

## 5. API 설계 규칙

- REST 원칙 준수. 자원은 명사 복수형, 행위는 HTTP 메서드로 표현.
- **모든 응답은 통일된 래퍼**를 사용한다.
  ```json
  // 성공
  { "success": true, "data": { ... } }
  // 실패
  { "success": false, "error": { "code": "MEMBER_NOT_FOUND", "message": "회원을 찾을 수 없습니다." } }
  ```
- HTTP 상태 코드를 의미대로 사용: `200/201`, `400`(검증), `401`(미인증), `403`(권한), `404`(없음), `409`(충돌), `500`(서버).
- 페이징이 필요한 목록은 `page`, `size`, `sort` 쿼리 파라미터를 표준으로 쓴다.
- 날짜/시간은 서버·API 전송 시 **ISO-8601(UTC)**, 표시할 때만 로컬 변환.

---

## 6. 상태 관리 원칙 (Frontend)

- **서버 상태와 클라이언트 상태를 분리한다.**
  - **서버 상태**(회원, 결제, 예약 등 API 데이터) → **TanStack Query**로 관리. 직접 `useState`에 담아 수동 관리하지 않는다.
    - 쿼리 키는 `['members', memberId]`처럼 배열 컨벤션 통일.
    - 변경(mutation) 후 관련 쿼리를 `invalidate`하여 동기화.
  - **전역 클라이언트 상태**(로그인 사용자, 테마 등) → **Zustand**.
  - **로컬 UI 상태**(모달 열림, 입력값) → 컴포넌트 `useState`.
- **폼 상태**는 React Hook Form으로 관리하고, 검증 스키마는 Zod로 정의한다. FE 검증은 UX 보조일 뿐, **최종 검증 책임은 항상 Backend**에 있다.
- 서버에서 이미 가공 가능한 값을 FE에서 중복 계산하지 않는다.

---

## 7. 에러 핸들링 원칙

### 7.1 Backend
- **커스텀 예외 계층**을 둔다. `BusinessException`을 기반으로 도메인 예외(`MemberNotFoundException`, `InsufficientPtCountException` 등)를 정의.
- **`@RestControllerAdvice` 전역 예외 핸들러**에서 모든 예외를 잡아 위의 통일된 에러 응답 포맷으로 변환한다. 컨트롤러마다 try-catch 흩뿌리지 않는다.
- 에러 코드는 `enum`으로 관리(예: `ErrorCode.MEMBER_NOT_FOUND`)하여 코드/메시지/HTTP상태를 한 곳에서 정의.
- 예상 가능한 실패는 예외로 명확히 표현하고, 삼키지(swallow) 않는다. 로그는 의미 있는 컨텍스트와 함께 남긴다.
- 트랜잭션이 필요한 로직(결제, 횟수 차감 등)은 `@Transactional`로 원자성을 보장한다.

### 7.2 Frontend
- Axios **인터셉터**에서 공통 처리: `401` → 로그인 만료 처리/리다이렉트, 공통 에러 토스트.
- TanStack Query의 `onError`/에러 상태로 화면별 에러 UI를 처리한다.
- 사용자에게는 **친절한 메시지**(서버의 `error.message` 활용)를, 콘솔/로그에는 상세를.
- 로딩/에러/빈 상태(empty state) 세 가지를 **항상** 고려해 UI를 만든다.

---

## 8. UI/UX 디자인 시스템 (Tailwind)

- **유틸리티 우선.** 커스텀 CSS 파일은 최소화하고 Tailwind 클래스로 스타일링한다.
- **디자인 토큰은 `tailwind.config`에서 중앙 관리.** 색상/간격/폰트를 하드코딩(`#3b82f6`)하지 말고 테마 토큰(`primary`, `danger` 등)을 정의해 사용한다.
  - 예: `primary`(브랜드), `secondary`, `danger`(삭제/경고), `success`(출석/완료), `muted`(보조 텍스트).
- **반복되는 UI는 공용 컴포넌트로.** 같은 `className` 뭉치를 3번 이상 복붙하면 `Button`, `Card`, `Table`, `Modal`, `Badge`, `Input` 등 컴포넌트로 추출한다.
- 버튼/입력/뱃지는 `variant`, `size` prop으로 변형을 관리(디자인 일관성).
- **반응형**은 모바일 우선(`sm:`, `md:`, `lg:`) 접근. 데스크 직원 태블릿 사용을 고려.
- **접근성:** 시맨틱 태그, `label`-`input` 연결, 버튼은 `<button>`, 키보드 포커스 유지.
- 간격/정렬은 임의 값 대신 스케일(4의 배수: `p-2`, `p-4`, `gap-4`) 위주로 통일.
- 아이콘은 한 라이브러리(예: `lucide-react`)로 통일.

---

## 9. 도메인 규칙 메모 (구현 시 주의)

- **회원 번호**는 출석 체크의 핵심 키. 유일성 보장, 조회 성능을 위해 인덱스 고려.
- **이용권/PT권**은 회원에 부여되는 별도 엔티티다. 회원 필드에 직접 박지 않는다.
  - 이용권: 시작일 + 개월 수 → 만료일 계산. 만료 여부 판단 로직은 Service에 둔다.
  - PT권: `totalCount` / `remainingCount`. 차감은 음수가 되지 않도록 검증(`InsufficientPtCountException`).
- **PT 예약**은 트레이너 스케줄과 회원 예약의 매칭이다. **시간 중복 예약을 서버에서 검증**한다(동시성 주의).
- **결제**는 이력(로그) 성격. 한번 기록된 결제는 임의 수정보다 취소/환불 이력을 남기는 방향으로 설계.
- 금액은 정수(원 단위) 또는 `BigDecimal`로 다룬다. 부동소수점 `float/double` 금지.

---

## 10. 테스트 & 검증

- 각 조각을 낸 뒤 **최소한의 검증 방법**을 함께 제시한다(빌드 통과, 해당 엔드포인트 curl 예시, 화면 확인 절차 등).
- Backend 핵심 비즈니스 로직(횟수 차감, 만료 계산, 예약 중복)은 **단위 테스트를 우선** 작성한다.
- "동작한다"고 말하기 전에 실제로 확인한 근거를 함께 밝힌다. 실패하면 그대로 보고한다.

---

## 11. 커밋 / 협업

- 커밋은 작고 논리적인 단위로. 메시지는 명령형 요약 (예: `feat: 회원 생성 API 추가`).
- 커밋/푸시는 **사용자가 요청할 때만** 수행한다.
- 타입 접두사: `feat`, `fix`, `refactor`, `docs`, `test`, `chore`.

---

## 12. 보안 기본

- 비밀번호는 **BCrypt** 등으로 해싱. 평문 저장 금지.
- JWT 시크릿, DB 비밀번호 등 비밀값은 **환경변수/`.env`**로 관리하고 절대 커밋하지 않는다.
- 관리자 인증/인가는 Spring Security 필터에서 일관되게 처리. 엔드포인트별 권한 명시.
- 모든 외부 입력은 서버에서 검증(`@Valid` + DTO 제약)한다.

---

## 13. Docker & 배포

### 13.1 기본 방침
- 로컬 개발부터 배포까지 **Docker로 환경을 통일**한다. "내 컴퓨터에선 되는데" 문제를 원천 차단한다.
- 구성요소 3개(**frontend / backend / db**)는 `docker-compose.yml` 하나로 오케스트레이션한다.
- 비밀값(DB 비밀번호, JWT 시크릿)은 `.env`로 주입하고 **절대 커밋하지 않는다**(§12와 동일). 대신 `.env.example`을 커밋해 필요한 키 목록을 공유한다.

### 13.2 도입 순서 (MVP 원칙 준수 — 한 번에 다 만들지 않는다)
1. **DB 컨테이너부터** — 로컬에 PostgreSQL 설치 없이 `docker compose up -d db`로 개발 시작.
2. 백엔드 완성 후 `backend/Dockerfile`(멀티스테이지: Gradle 빌드 → JRE 실행) 추가.
3. 프론트 완성 후 `frontend/Dockerfile`(멀티스테이지: Vite 빌드 → Nginx 정적 서빙) + `nginx.conf`(SPA 라우팅 + `/api` 프록시) 추가.
4. 마지막에 전체 Compose로 묶어 배포.

### 13.3 저장소 레이아웃 (배포 관련)
```
gym_project/
├── docker-compose.yml        # 3개 서비스 오케스트레이션
├── .env                      # 비밀값 (커밋 금지)
├── .env.example              # 필요한 키 목록 (커밋)
├── frontend/
│   ├── Dockerfile            # Vite 빌드 → Nginx 서빙
│   └── nginx.conf            # SPA 라우팅 + /api 프록시
└── backend/
    └── Dockerfile            # Gradle 빌드 → JRE 실행
```

### 13.4 규칙
- **포트 매핑:** DB는 호스트로 `5432:5432` 노출해 **DBeaver 등 GUI 툴로 접근 가능**하게 한다. 로컬 PostgreSQL과 충돌 시 호스트 포트만 변경(`5433:5432`).
- **DB 접속 주소 주의:** 호스트 툴(DBeaver)은 `localhost`로, **백엔드 컨테이너는 Compose 서비스 이름 `db`로** 접속한다. 같은 DB라도 주소가 다르다.
- **데이터 영속화:** DB는 named volume(`gym_db_data`)으로 데이터를 보존한다. 컨테이너를 지워도 데이터가 날아가지 않게 한다.
- **이미지 최적화:** FE/BE 모두 **멀티스테이지 빌드**로 최종 이미지를 가볍게 유지한다. 빌드 도구는 런타임 이미지에 포함하지 않는다.
- **환경 분리:** 개발/운영 설정(포트, 로그레벨, DB 접속 정보)은 환경변수로 분리한다. Spring은 프로파일(`application-*.yml`)로 관리.

---

### 요약: Claude가 이 저장소에서 일할 때
1. 작업 전 **계획을 먼저** 보여주고 동의를 받는다.
2. **파일 1개 단위(MVP)**로 만들고 **멈춰서 검증**한다.
3. 위 스택/구조/네이밍/에러·상태 규칙을 지킨다.
4. 모호하면 **추측하지 말고 질문**한다.
5. 요청받지 않은 것은 만들지 않는다.
