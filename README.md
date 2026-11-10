# music-review-server

Spotify의 곡·앨범·플레이리스트에 별점과 리뷰를 남기는 안드로이드 앱의 **백엔드 서버**입니다.
음악 정보는 앱이 Spotify Web API에서 직접 가져오고, 이 서버는 **회원, 리뷰, 통계**를 담당합니다.

| 항목 | 내용 |
|---|---|
| 언어 / 런타임 | Java 21 (Temurin) |
| 프레임워크 | Spring Boot 4.1.2 (Web MVC, Data JPA, Security, Validation, OAuth2 Resource Server) |
| DB | MySQL 8.4 (개발 초기에는 H2 메모리 DB) |
| 빌드 | Gradle (wrapper 포함) |
| 인증 | Spotify Access Token 확인 → 서버 자체 JWT(HS256) 발급 |

## 문서

- [API 명세서](docs/API.md)
- [DB 스키마 (MySQL DDL)](docs/schema.sql)
- [HTTP 요청 모음 (IntelliJ HTTP Client)](http/api.http)
- [Postman 컬렉션](postman/) — 아래 「Postman으로 테스트」 참고

## Postman으로 테스트

1. Postman → **Import** → `postman/` 폴더의 두 파일을 끌어다 놓기
   - `music-review-server.postman_collection.json` (요청 22개, 자동 테스트 포함)
   - `local.postman_environment.json` (환경: `music-review local`)
2. 오른쪽 위 환경 선택에서 **music-review local** 선택
3. 환경 변수 `spotifyToken`에 Spotify Access Token 입력
4. 서버 실행 후 **0. 기본 → 서버 동작 확인**, **1. 인증·회원 → 로그인** 순서로 실행
   - 로그인하면 `jwt`, `myId`가 자동 저장되고, 나머지 요청은 컬렉션 공통 `Bearer {{jwt}}` 인증을 씁니다.
   - 리뷰 작성하면 `reviewId`가 저장돼 조회·수정·삭제에 이어서 쓰입니다.
5. 폴더 전체를 실행하려면 컬렉션 `...` → **Run collection**

`[실패]`로 시작하는 요청은 에러 응답(400/401/409)이 명세대로 나오는지 확인하는 용도입니다.

> 토큰이 들어간 환경을 다시 Export해서 커밋하지 마세요. 저장소의 환경 파일은 값이 비어 있어야 합니다.

## 구조

```
앱(Android) ──Spotify 토큰──▶ POST /api/auth/spotify ──▶ Spotify GET /v1/me (사용자 확인)
     │                              │
     │◀────────── 서버 JWT ─────────┘
     │
     ├──(Spotify 토큰)──▶ Spotify Web API : 검색, 곡/앨범/플리 정보, 재생 기록, 팔로우
     └──(서버 JWT)─────▶ 이 서버 : 리뷰 CRUD, 평균 별점, 피드, 프로필, 랭킹 ──▶ MySQL
```

## 패키지

```
com.musicreview.server
├─ global
│  ├─ config      SecurityConfig, CORS, JPA Auditing, RestClient
│  ├─ security    JWT 발급/검증
│  ├─ exception   ErrorCode, GlobalExceptionHandler
│  └─ common      BaseTimeEntity, PageResponse
├─ auth           로그인 (F01)
├─ user           내 정보, 유저 리뷰/통계 (F01, F09)
├─ review         리뷰 CRUD, 태그 (F06, F07, F10)
├─ item           항목별 리뷰 목록, 요약 (F04, F07, F11)
├─ feed           최신 피드, 인기 항목 (F08)
├─ curator        큐레이터 랭킹 (F12)
└─ spotify        Spotify API 클라이언트
```

각 기능 패키지 안은 `controller / service / repository / domain / dto` 순서로 채웁니다.

## 실행

### 1) local 프로필 (기본값, H2 메모리 DB)

설치할 것 없이 바로 실행됩니다. 서버를 끄면 데이터가 사라집니다.

```bash
./gradlew bootRun
```

- 서버: http://localhost:8080
- H2 콘솔: http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:music_review`, 사용자 `sa`)

### 2) mysql 프로필 (로컬 MySQL)

처음 한 번 DB를 만듭니다.

```powershell
# PowerShell (프로젝트 폴더에서 실행)
mysql -u root -p -e "source docs/schema.sql"
```

> cmd나 Git Bash에서는 `mysql -u root -p < docs/schema.sql`도 됩니다. PowerShell은 `<`를 지원하지 않습니다.

환경변수를 넣고 실행합니다. (PowerShell 기준)

```powershell
$env:SPRING_PROFILES_ACTIVE="mysql"
$env:DB_PASSWORD="MySQL root 비밀번호"
$env:JWT_SECRET="32자 이상 임의 문자열"
./gradlew bootRun
```

IntelliJ에서는 **Run → Edit Configurations → Environment variables**에 같은 값을 넣으면 됩니다.

| 환경변수 | 기본값 | 설명 |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `local` | `local` 또는 `mysql` |
| `DB_HOST` / `DB_PORT` / `DB_NAME` | `localhost` / `3306` / `music_review` | mysql 프로필 |
| `DB_USERNAME` | `root` | mysql 프로필 |
| `DB_PASSWORD` | (없음) | mysql 프로필, **필수** |
| `JWT_SECRET` | local만 개발용 기본값 | 32자 이상, mysql 프로필에서는 **필수** |

> 비밀번호나 키는 절대 커밋하지 마세요. `.env`, `http/http-client.private.env.json`은 `.gitignore`에 등록돼 있습니다.

### 안드로이드 에뮬레이터에서 접속

에뮬레이터 안에서 PC의 `localhost`는 `http://10.0.2.2:8080`입니다.

## 개발 순서

| 단계 | 내용 | 상태 |
|---|---|---|
| 1 | 준비: 프로젝트 생성, API 명세, DB 스키마 | ✅ |
| 2 | 서버 기초: `/api/hello`, User/Review 엔티티, 리뷰 CRUD, 검증, 공통 예외 처리 | ⬜ |
| 3 | 로그인: `/api/auth/spotify`, JWT 발급, Security 필터 | ⬜ |
| 4 | 항목 조회: 리뷰 목록, summary | ⬜ |
| 5 | H2 → MySQL 전환 | ⬜ |
| 6 | 권장 기능: 피드, 인기 항목, 프로필 통계, 내 리뷰 일괄 조회 | ⬜ |
| 7 | 추가 기능: 큐레이터 랭킹, 마무리 | ⬜ |

## 참고

- Spotify 개발 모드라 Dashboard에 등록한 테스트 유저(최대 25명)만 로그인할 수 있습니다.
- Spotify 콘텐츠를 보여줄 때는 Spotify 로고와 원본 링크를 표시해야 합니다(브랜딩 가이드라인).
