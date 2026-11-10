# PLIVIEW server

Spotify의 곡·앨범·플레이리스트에 별점과 리뷰를 남기는 앱 **PLIVIEW**의 백엔드 서버입니다.
클라이언트는 Flutter(안드로이드 앱 + 웹)이고, 이 서버는 **회원, 리뷰, 통계, Spotify 대리 조회**를 담당합니다.

| 항목 | 내용 |
|---|---|
| 언어 / 런타임 | Java 21 (Temurin) |
| 프레임워크 | Spring Boot 4.1.2 (Web MVC, Data JPA, Security, Validation, OAuth2 Resource Server) |
| DB | MySQL 8.4 (기본 실행·테스트는 H2 메모리 DB) |
| 빌드 | Gradle (wrapper 포함) |
| 인증 | 이메일 + 비밀번호(BCrypt) → 서버 JWT(HS256, 7일) |
| 테스트 | MockMvc 통합 테스트 46개 (`./gradlew test`) |

## 문서

- [API 명세서](docs/API.md)
- [DB 스키마 (MySQL DDL)](docs/schema.sql)
- [HTTP 요청 모음 (IntelliJ HTTP Client)](http/api.http)
- [Postman 컬렉션](postman/) — 아래 「Postman으로 테스트」 참고

## 구조

```
Flutter 앱/웹 ──이메일·비밀번호──▶ POST /api/auth/signup, /login ──▶ 서버 JWT 발급
     │
     ├──(서버 JWT)──▶ 이 서버
     │                 ├─ 회원·리뷰·평균·분포 ─────────────▶ MySQL
     │                 └─ /api/spotify/** ──(앱 토큰, Client Credentials)──▶ Spotify Web API
     │                                      검색, 곡·앨범·공개 플리 정보
     │
     └──(사용자 Spotify 토큰, PKCE)──▶ Spotify Web API (앱이 직접, 선택 기능)
                                        최근 재생, 내 플리, 플리 팔로우·생성
```

## 패키지

```
com.musicreview.server
├─ global
│  ├─ config      SecurityConfig(CORS, BCrypt), JPA Auditing, WebMvc
│  ├─ security    JWT 발급/검증, @CurrentUserId, 401/403 JSON 응답
│  ├─ exception   ErrorCode, GlobalExceptionHandler
│  ├─ validation  @Nickname, @Password, @Score, @SpotifyId
│  └─ common      BaseTimeEntity, PageResponse, /api/hello
├─ auth           회원가입, 로그인
├─ user           내 정보, 비밀번호 변경, 탈퇴, 유저 프로필(리뷰·통계)
├─ review         리뷰 CRUD, 태그, 내 리뷰 일괄 조회, 집계 쿼리
├─ item           항목별 리뷰 목록, 요약(평균·분포·내 리뷰)
├─ spotify        Spotify 대리 조회 (검색, 곡·앨범·플리)
├─ feed           최신 피드, 인기 항목
└─ curator        큐레이터 랭킹
```

## 실행

### 1) local 프로필 (기본값, H2 메모리 DB)

설치할 것 없이 바로 실행됩니다. 서버를 끄면 데이터가 사라집니다.

```bash
./gradlew bootRun
```

- 서버: http://localhost:8080 (확인: http://localhost:8080/api/hello)
- H2 콘솔: http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:music_review`, 사용자 `sa`)

### 2) mysql 프로필 (로컬 MySQL)

처음 한 번 DB를 만듭니다. (예전 v1 스키마로 만든 DB가 있으면 `docs/schema.sql` 맨 위 안내대로 지우고 다시 만드세요.)

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
| `JWT_SECRET` | local만 개발용 기본값 | 32바이트 이상, mysql 프로필에서는 **필수** |
| `SPOTIFY_CLIENT_ID` / `SPOTIFY_CLIENT_SECRET` | (없음) | Spotify Dashboard에서 발급. 없으면 `/api/spotify/**`만 503 |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:*,http://127.0.0.1:*` | Flutter 웹 배포 주소를 쉼표로 추가 |

> 비밀번호나 키는 절대 커밋하지 마세요. `.env`, `http/http-client.private.env.json`, Postman 로컬 워크스페이스 파일은 `.gitignore`에 등록돼 있습니다.

### Flutter에서 접속

| 실행 환경 | Base URL |
|---|---|
| Flutter 웹 (Chrome) | `http://localhost:8080` (CORS 허용됨) |
| 안드로이드 에뮬레이터 | `http://10.0.2.2:8080` |
| 실제 폰 (같은 Wi-Fi) | `http://<PC의 IP>:8080` |

## Postman으로 테스트

1. Postman → **Import** → `postman/` 폴더의 두 파일을 끌어다 놓기
   - `music-review-server.postman_collection.json`
   - `local.postman_environment.json` (환경: `PLIVIEW local`)
2. 오른쪽 위 환경 선택에서 **PLIVIEW local** 선택
3. 서버 실행 후 **1. 인증·회원 → 회원가입**(처음) 또는 **로그인** 실행
   - `jwt`, `myId`가 자동 저장되고, 나머지 요청은 컬렉션 공통 `Bearer {{jwt}}` 인증을 씁니다.
   - 리뷰 작성하면 `reviewId`가 저장돼 조회·수정에 이어서 쓰입니다.
4. 폴더 전체를 실행하려면 컬렉션 `...` → **Run collection** (`9. 탈퇴` 폴더는 빼고)

`[실패]`로 시작하는 요청은 에러 응답(400/401/409)이 명세대로 나오는지 확인하는 용도입니다.

## 개발 현황

| 단계 | 내용 | 상태 |
|---|---|---|
| 1 | 준비: 프로젝트 생성, API 명세, DB 스키마 | ✅ |
| 2 | 공통: 에러 응답, 검증, 시간 기록, 페이지 응답, `/api/hello` | ✅ |
| 3 | 인증: 회원가입, 로그인, JWT, Security, CORS | ✅ |
| 4 | 회원: 내 정보, 비밀번호 변경, 탈퇴 | ✅ |
| 5 | 리뷰: 작성·조회·수정·삭제, 내 리뷰 일괄 조회 | ✅ |
| 6 | 항목: 리뷰 목록(정렬·페이지), 요약(평균·분포·내 리뷰) | ✅ |
| 7 | Spotify 대리 조회: 검색, 곡·앨범·아티스트 앨범·플리 | ✅ |
| 8 | 권장: 피드, 인기 항목, 유저 리뷰·통계 | ✅ |
| 9 | 추가: 큐레이터 랭킹 | ✅ |
| 10 | 배포, CI(GitHub Actions) | ⬜ |

## 참고

- Spotify 콘텐츠를 보여줄 때는 Spotify 로고와 원본 링크를 표시해야 합니다(브랜딩 가이드라인).
- 사용자 본인 Spotify 계정이 필요한 기능(최근 재생 등)은 앱이 Spotify 연동(PKCE)으로 직접 호출하며, Spotify 개발 모드에서는 Dashboard에 등록한 테스트 유저(최대 25명)만 연동할 수 있습니다.
