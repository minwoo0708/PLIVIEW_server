<div align="center">

# 🎧 PLIVIEW

**듣고, 점수 주고, 같이 고르는 플리**

Spotify의 곡 · 앨범 · 플레이리스트에 별점과 리뷰를 남기는 앱 **PLIVIEW**의 백엔드 서버

![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1-6DB33F?logo=springboot&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-8.4-4479A1?logo=mysql&logoColor=white)
![Gradle](https://img.shields.io/badge/Gradle-9-02303A?logo=gradle&logoColor=white)
![Flyway](https://img.shields.io/badge/Flyway-12-CC0200?logo=flyway&logoColor=white)
![JWT](https://img.shields.io/badge/Auth-JWT%20%2B%20BCrypt-000000?logo=jsonwebtokens&logoColor=white)
![Spotify](https://img.shields.io/badge/Spotify%20Web%20API-1DB954?logo=spotify&logoColor=white)
![Tests](https://img.shields.io/badge/integration%20tests-68-brightgreen)

[📱 Flutter 앱 저장소](https://github.com/minwoo0708/PLIVIEW_app) · [📘 API 명세서](docs/API.md) · [🗄 DB 스키마](docs/schema.sql)

</div>

<br>

<img src="docs/design/overview.png" alt="PLIVIEW 앱 화면 디자인" width="100%">

<p align="center"><sub>앱 화면 디자인 · 시작, 홈, 탐색, 재생 기록, 프로필, 앨범·플리 상세, 평가하기, 큐레이터 랭킹, 로그인·가입, 설정</sub></p>

> 곡도 앨범도 플레이리스트도 **내 기준으로 기록**해요. 음악 정보는 Spotify에서 가져오고,
> 회원·리뷰·통계는 이 서버가 맡습니다. 클라이언트는 **Flutter 하나로 만든 안드로이드 앱과 웹**이에요.


## ✨ 주요 기능

| | 기능 | 설명 |
|---|---|---|
| 🔐 | **계정** | 이메일 회원가입·로그인(JWT), 닉네임·프로필 사진 수정, 비밀번호 변경, 회원 탈퇴 |
| ⭐ | **리뷰** | 곡·앨범·플리에 **별점(0.5 단위)** · 한 줄 평 · 태그. 항목당 1인 1리뷰, 본인 리뷰만 수정·삭제 |
| 💿 | **앨범·플리 평가** | 최고의 곡 · 워스트 곡 · 과소평가된 곡(앨범만) · 최고의 순간 · 전체 평가(여러 줄) |
| 🎚 | **플리 전용 점수** | 컨셉 일치도 · 곡 흐름 · 발견성 + 총점. 리뷰 뒤 플리가 바뀌었는지 알 수 있게 `snapshot_id`를 함께 저장 |
| 📊 | **통계** | 항목별 평균과 별점 분포(10칸), 유저 프로필 통계, 곡별 평점 일괄 조회 |
| 🏠 | **홈** | 최신 리뷰 피드, 요즘 리뷰 많은 항목, 이번 주의 플리 |
| 🏆 | **베스트 큐레이터** | Spotify를 연동한 사용자가 만든 플리의 평균 점수 순위(동점은 같은 순위), 큐레이터 프로필, 인기순 플리 |
| 🎧 | **Spotify 대리 조회** | 검색 · 곡 · 앨범 · 아티스트 앨범 · 플리 정보를 서버가 대신 조회. 앱은 Spotify 로그인 없이도 음악 정보를 봅니다 |
| 🔗 | **Spotify 계정 연동** | 앱이 PKCE로 받은 토큰을 서버가 확인만 하고 **저장하지 않습니다** |

## 🏗 아키텍처

```mermaid
flowchart LR
    subgraph Client["📱 Flutter (Android · Web)"]
        App["PLIVIEW 앱"]
    end
    subgraph Server["🍃 Spring Boot (이 저장소)"]
        API["REST API<br/>JWT 인증 · 입력 검증"]
        Proxy["Spotify 대리 조회<br/>응답 캐시 · 요청 제한"]
    end
    DB[("MySQL 8.4<br/>Flyway 마이그레이션")]
    Spotify["🎧 Spotify Web API"]

    App -- "Bearer JWT" --> API
    API --> DB
    API --> Proxy
    Proxy -- "앱 토큰<br/>(Client Credentials)" --> Spotify
    App -. "사용자 토큰(PKCE)<br/>최근 재생" .-> Spotify
```

음악 정보는 서버가 **자기 Spotify 앱 자격 증명**으로 대신 가져와서, 웹 앱에 Client Secret을 둘 필요가 없어요.
사용자 본인의 Spotify 권한이 필요한 기능(최근 재생)만 앱이 PKCE로 Spotify에 직접 요청합니다.

### 리뷰를 쓰면 일어나는 일

```mermaid
sequenceDiagram
    participant App as 📱 앱
    participant API as Spring Boot
    participant SP as 🎧 Spotify
    participant DB as 🗄 MySQL

    App->>API: POST /api/reviews (JWT · 별점 · 점수 · 태그)
    API->>API: 입력 검증 (0.5 단위 · 타입별 필드 규칙)
    API->>SP: 곡·앨범·플리 정보 확인 (2분 캐시)
    SP-->>API: 이름 · 커버 · 플리 주인
    Note over API: 앱이 보낸 값보다 Spotify 값을 우선 저장
    API->>DB: INSERT (항목당 1개 UNIQUE)
    DB-->>API: 저장됨
    API-->>App: 201 Created + 리뷰
```


## 🗄 데이터 모델

```mermaid
erDiagram
    USERS ||--o{ REVIEWS : "작성"
    REVIEWS ||--o{ REVIEW_TAGS : "태그"

    USERS {
        bigint id PK
        varchar email UK
        varchar password_hash "BCrypt"
        varchar nickname
        varchar spotify_user_id UK "연동한 Spotify 계정"
    }
    REVIEWS {
        bigint id PK
        bigint user_id FK
        varchar type "TRACK, ALBUM, PLAYLIST"
        varchar spotify_id
        decimal rating "0.5 ~ 5.0"
        varchar comment "한 줄 평"
        decimal concept_score "플리 전용"
        decimal flow_score "플리 전용"
        decimal discovery_score "플리 전용"
        varchar snapshot_id "플리 전용"
        varchar owner_spotify_id "플리 주인"
        varchar best_track_id "앨범·플리"
        varchar best_moment "앨범·플리"
        varchar body "전체 평가"
    }
    REVIEW_TAGS {
        bigint review_id PK, FK
        varchar tag PK
    }
```

- `reviews`는 `(user_id, type, spotify_id)`가 **UNIQUE**라서 한 항목에 한 사람이 리뷰를 하나만 쓸 수 있어요.
- 평균·별점 분포·랭킹은 별도 통계 테이블 없이 **SQL 집계**(`AVG`, `GROUP BY`)로 계산합니다.
- 피드와 이름·이미지 표시용으로 리뷰에 항목 정보(`item_name`, `item_subtitle`, `item_image_url`)를 함께 저장해, 목록마다 Spotify를 다시 부르지 않아요.

## 🧰 기술 스택

| 영역 | 사용 기술 | 이유 |
|---|---|---|
| 언어 · 런타임 | **Java 21** (Temurin) | LTS, record로 DTO를 간결하게 |
| 프레임워크 | **Spring Boot 4.1** (Web MVC, Data JPA, Validation) | REST API와 영속성 |
| 인증 · 보안 | **Spring Security 7** · OAuth2 Resource Server(JWT, HS256) · BCrypt | 세션 없는 stateless 인증 |
| DB | **MySQL 8.4** (개발·테스트는 H2) | 실제 DB로 확인, 테스트는 빠르게 |
| 마이그레이션 | **Flyway** | 스키마 변경 이력을 파일로 관리 |
| 캐시 | **Caffeine** | Spotify 응답 · 요청 제한 카운터 |
| 외부 연동 | **Spotify Web API** (`RestClient`) | 검색 · 곡 · 앨범 · 플리 정보 |
| 빌드 · 테스트 | **Gradle** · JUnit 5 · MockMvc | 통합 테스트 68개 |
| 협업 도구 | Postman · IntelliJ HTTP Client | 요청 모음 포함 |


## 🔌 API 한눈에 보기

모든 에러는 같은 JSON 형식(`status`, `code`, `message`, `errors`, `timestamp`)으로 응답하고, 목록은 같은 페이지 형식을 씁니다.
요청·응답 예시와 에러 코드는 **[docs/API.md](docs/API.md)**에 있어요.

<details>
<summary><b>API 29개 전체 보기</b></summary>

<br>

| 영역 | 메서드 · 경로 | 설명 |
|---|---|---|
| 확인 | `GET /api/hello` | 서버 동작 확인 |
| 계정 | `POST /api/auth/signup` | 회원가입 (가입 즉시 로그인) |
| | `POST /api/auth/login` | 로그인 |
| | `GET` `PATCH /api/users/me` | 내 정보 · 닉네임·프로필 사진 수정 |
| | `PATCH /api/users/me/password` | 비밀번호 변경 |
| | `DELETE /api/users/me` | 회원 탈퇴 (리뷰도 삭제) |
| | `PUT` `DELETE /api/users/me/spotify` | Spotify 계정 연동 · 해제 |
| Spotify | `GET /api/spotify/search` | 곡·앨범·아티스트·플리 검색 |
| | `GET /api/spotify/tracks/{id}` | 곡 정보 |
| | `GET /api/spotify/albums/{id}` | 앨범 정보 + 수록곡 |
| | `GET /api/spotify/artists/{id}/albums` | 아티스트의 앨범 |
| | `GET /api/spotify/playlists/{id}` | 플리 정보 |
| 리뷰 | `POST /api/reviews` | 리뷰 작성 |
| | `GET` `PATCH` `DELETE /api/reviews/{id}` | 조회 · 수정 · 삭제 |
| | `GET /api/reviews/mine` | 여러 항목에 대한 내 리뷰 일괄 조회 |
| 항목 | `GET /api/items/{type}/{id}/reviews` | 항목의 리뷰 목록 (정렬·페이지) |
| | `GET /api/items/{type}/{id}/summary` | 평균 · 별점 분포 · 내 리뷰 |
| | `GET /api/items/tracks/ratings` | 곡별 평점 일괄 조회 |
| 홈 · 프로필 | `GET /api/feed` | 최신 리뷰 피드 |
| | `GET /api/feed/popular` | 리뷰 많은 항목 |
| | `GET /api/feed/weekly-playlists` | 이번 주의 플리 |
| | `GET /api/users/{id}/reviews` | 유저의 리뷰 + 통계 |
| 큐레이터 | `GET /api/curators/ranking` | 베스트 큐레이터 순위 |
| | `GET /api/curators/{id}` | 큐레이터 프로필 |
| | `GET /api/curators/{id}/playlists` | 큐레이터의 플리 (인기순) |

</details>

## 🧠 설계에서 신경 쓴 점

**🔐 인증 · 보안**
- 세션 없이 **JWT(HS256, 7일)** 로 인증하고 비밀번호는 **BCrypt**로만 저장합니다. 응답에 비밀번호가 나오지 않아요.
- 필터 단계의 401·403도 서버 공통 에러 형식으로 응답해, 앱이 에러를 한 가지 방식으로 처리할 수 있어요.
- 로그인 실패는 "이메일이 없는지 / 비밀번호가 틀렸는지"를 알려 주지 않습니다.
- Flutter 웹이 다른 포트에서 부를 수 있게 CORS를 설정했고, 배포 주소는 환경변수로 추가합니다.

**✅ 입력 검증 · 정합성**
- 별점·닉네임·비밀번호·Spotify id는 **커스텀 검증 어노테이션**(`@Score`, `@Nickname`, `@Password`, `@SpotifyId`)으로 한 곳에서 관리합니다.
- 곡에 플리 전용 항목을 보내거나, 플리에서 필수 항목이 빠지면 거절합니다.
- "항목당 1인 1리뷰"는 UNIQUE 제약으로 보장하고, 동시에 두 번 요청해도 한 번만 저장됩니다.
- 수정 요청의 `clear`로 선택 항목을 명시적으로 지울 수 있어서, "보내지 않음"과 "지움"이 헷갈리지 않아요.

**🎧 Spotify 연동**
- 앱 토큰은 만료 직전까지 재사용하고, 유효한 토큰이 있으면 **잠금 없이** 바로 돌려줍니다.
- 같은 요청은 **2분 캐시**, 사용자당 **1분 60회 제한**으로, 모든 사용자가 토큰 하나를 같이 써도 한 명의 연타가 전체에 번지지 않게 했어요.
- 쿼리를 엄격하게 인코딩해서 `C++` 같은 검색어도 정확히 전달하고, Spotify의 오류를 우리 에러 코드로 구분해 돌려줍니다.
- **신뢰할 수 없는 입력 방어**: 리뷰를 저장할 때 서버가 Spotify에서 이름·커버·플리 주인을 다시 확인해, 앱이 보낸 값으로 큐레이터 랭킹을 조작하지 못하게 합니다.

**⚡ 성능 · 운영**
- 인기 항목과 큐레이터 목록의 표시 정보를 항목마다 조회하지 않고 **타입별로 묶어서** 한 번에 가져옵니다(N+1 제거).
- 타입별 피드는 쿼리를 나누고 인덱스를 추가했어요.
- 스키마는 **Flyway**로만 바꾸고, mysql 프로필의 Hibernate는 엔티티와 테이블이 맞는지 **검사만**(`validate`) 합니다.

**🧪 테스트**
- 실제 HTTP 요청처럼 호출하는 **MockMvc 통합 테스트 68개**. 인증, 검증, 권한, 집계, 페이지, 에러 형식을 확인합니다.
- Spotify는 테스트 안에서 띄운 **가짜 HTTP 서버**로 대신해서, 토큰 캐시 · 응답 캐시 · 쿼리 인코딩 · 오류 변환을 외부 의존 없이 검증합니다.

