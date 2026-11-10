# PLIVIEW — REST API 명세서

> 서버: Spring Boot 4.1 / Java 21 / MySQL 8.4
> 클라이언트: Flutter (안드로이드 앱 + 웹)
> 버전: v0.2 (자체 회원가입·로그인 방식)

---

## 1. 공통 규칙

### 1.1 기본 정보

| 항목 | 값 |
|---|---|
| Base URL (PC, Flutter 웹) | `http://localhost:8080` |
| Base URL (안드로이드 에뮬레이터) | `http://10.0.2.2:8080` |
| Content-Type | `application/json; charset=UTF-8` |
| 날짜/시간 | ISO-8601, 한국 시간 (`2026-11-10T14:30:00+09:00`) |
| ID | 서버 리소스 id는 `Long`, Spotify 리소스 id는 `String` (예: `4uLU6hMCjMI75M1A2tKUQC`) |
| CORS | Flutter 웹 개발 서버(`http://localhost:*`, `http://127.0.0.1:*`) 허용. 배포 주소는 `CORS_ALLOWED_ORIGINS`로 추가 |

### 1.2 인증

- 회원가입·로그인은 **이메일 + 비밀번호**로 서버가 직접 처리합니다. Spotify 계정이 없어도 가입할 수 있습니다.
- `인증` 열이 ✅인 API는 헤더가 필요합니다.
  ```
  Authorization: Bearer <회원가입·로그인 응답의 accessToken>
  ```
- JWT: HS256, 유효기간 7일, `sub` = 서버 user id. 만료되면 다시 로그인합니다(리프레시 토큰 없음).
- 토큰이 없거나 만료·위조: `401 UNAUTHORIZED`.
- 로그아웃은 서버 호출 없이 **앱이 저장한 토큰을 지우면** 됩니다.
- 비밀번호는 BCrypt로 해시해서 저장하고, 응답에 절대 포함하지 않습니다.

### 1.3 리뷰 대상 타입 (`type`)

| 값 | 의미 | 경로 변수 표기 |
|---|---|---|
| `TRACK` | 곡 | `track` |
| `ALBUM` | 앨범 | `album` |
| `PLAYLIST` | 플레이리스트 | `playlist` |

- JSON 본문과 응답에서는 **대문자**(`TRACK`), URL 경로에서는 **소문자**(`/api/items/track/...`)를 씁니다. 서버는 경로와 쿼리의 대소문자를 구분하지 않습니다.

### 1.4 점수 규칙

- 모든 점수(`rating`, `conceptScore`, `flowScore`, `discoveryScore`)는 **0.5 ~ 5.0, 0.5 단위**입니다.
- 플레이리스트의 `rating`은 사용자가 직접 매기는 **총점**입니다. 세 항목의 평균으로 자동 계산하지 않습니다.
- 평균값 응답은 소수 둘째 자리에서 반올림한 **소수 첫째 자리**입니다(예: `4.3`). 리뷰가 없으면 `null`.

### 1.5 페이지네이션

요청 쿼리: `?page=0&size=20` (`page`는 0부터, `size` 기본 20, 1~50)

응답 형식:
```json
{
  "content": [ ... ],
  "page": 0,
  "size": 20,
  "totalElements": 134,
  "totalPages": 7,
  "hasNext": true
}
```

### 1.6 에러 응답

모든 에러는 아래 형식입니다.
```json
{
  "status": 400,
  "code": "INVALID_INPUT",
  "message": "입력값이 올바르지 않습니다.",
  "errors": [
    { "field": "rating", "reason": "0.5부터 5.0까지 0.5 단위로 입력해야 합니다." }
  ],
  "timestamp": "2026-11-10T14:30:00+09:00"
}
```
`errors`는 검증 실패(400)일 때만 채워지고, 그 외에는 빈 배열입니다.

| HTTP | code | 상황 |
|---|---|---|
| 400 | `INVALID_INPUT` | 필수값 누락, 범위·형식 오류, 잘못된 JSON |
| 400 | `INVALID_REVIEW_FIELDS` | 곡/앨범 리뷰에 플리 전용 필드를 보냄, 또는 플리 리뷰에 필수 항목 누락 |
| 400 | `INVALID_ITEM_TYPE` | 경로·쿼리의 `type`이 track/album/playlist가 아님 |
| 400 | `PASSWORD_MISMATCH` | 비밀번호 변경·탈퇴 시 현재 비밀번호가 틀림 |
| 401 | `UNAUTHORIZED` | JWT 없음, 만료, 위조 |
| 401 | `LOGIN_FAILED` | 이메일 또는 비밀번호가 틀림 (어느 쪽인지 알려 주지 않음) |
| 403 | `REVIEW_FORBIDDEN` | 남의 리뷰를 수정·삭제하려 함 |
| 404 | `USER_NOT_FOUND` | 없는 사용자 (탈퇴한 사용자의 토큰 포함) |
| 404 | `REVIEW_NOT_FOUND` | 없는 리뷰 |
| 404 | `SPOTIFY_NOT_FOUND` | Spotify에 없는 곡·앨범·플리 (비공개 플리 포함) |
| 404 | `NOT_FOUND` | 없는 API 경로 |
| 405 | `METHOD_NOT_ALLOWED` | 지원하지 않는 HTTP 메서드 |
| 409 | `EMAIL_ALREADY_EXISTS` | 이미 가입된 이메일 |
| 409 | `REVIEW_ALREADY_EXISTS` | 같은 항목에 이미 리뷰를 씀 (항목당 1개) |
| 429 | `SPOTIFY_RATE_LIMITED` | Spotify 호출 한도 초과, 잠시 후 재시도 |
| 502 | `SPOTIFY_UNAVAILABLE` | Spotify 서버 오류·시간 초과 |
| 503 | `SPOTIFY_NOT_CONFIGURED` | 서버에 Spotify Client ID/Secret이 설정되지 않음 |
| 500 | `INTERNAL_ERROR` | 그 외 서버 오류 |

---

## 2. API 목록

| # | 메서드 | 경로 | 설명 | 인증 | 우선순위 |
|---|---|---|---|---|---|
| 0 | GET | `/api/hello` | 서버 동작 확인 | ❌ | 필수 |
| 1 | POST | `/api/auth/signup` | 회원가입 (가입 즉시 로그인) | ❌ | 필수 |
| 2 | POST | `/api/auth/login` | 로그인 | ❌ | 필수 |
| 3 | GET | `/api/users/me` | 내 정보 | ✅ | 필수 |
| 4 | PATCH | `/api/users/me` | 닉네임·프로필 사진 수정 | ✅ | 필수 |
| 5 | PATCH | `/api/users/me/password` | 비밀번호 변경 | ✅ | 필수 |
| 6 | DELETE | `/api/users/me` | 회원 탈퇴 | ✅ | 필수 |
| 7 | GET | `/api/spotify/search` | 곡·앨범·아티스트·플리 검색 | ✅ | 필수 |
| 8 | GET | `/api/spotify/tracks/{id}` | 곡 정보 | ✅ | 필수 |
| 9 | GET | `/api/spotify/albums/{id}` | 앨범 정보 + 수록곡 | ✅ | 필수 |
| 10 | GET | `/api/spotify/artists/{id}/albums` | 아티스트의 앨범 목록 | ✅ | 필수 |
| 11 | GET | `/api/spotify/playlists/{id}` | 플리 정보 + 수록곡 | ✅ | 필수 |
| 12 | POST | `/api/reviews` | 리뷰 작성 | ✅ | 필수 |
| 13 | GET | `/api/reviews/{reviewId}` | 리뷰 단건 조회 | ✅ | 필수 |
| 14 | PATCH | `/api/reviews/{reviewId}` | 본인 리뷰 수정 | ✅ | 필수 |
| 15 | DELETE | `/api/reviews/{reviewId}` | 본인 리뷰 삭제 | ✅ | 필수 |
| 16 | GET | `/api/reviews/mine` | 여러 항목에 대한 내 리뷰 일괄 조회 | ✅ | 필수 |
| 17 | GET | `/api/items/{type}/{spotifyId}/reviews` | 항목의 리뷰 목록 | ✅ | 필수 |
| 18 | GET | `/api/items/{type}/{spotifyId}/summary` | 평균 별점, 분포, 내 리뷰 | ✅ | 필수 |
| 19 | GET | `/api/feed` | 최신 리뷰 피드 | ✅ | 권장 |
| 20 | GET | `/api/feed/popular` | 리뷰 많은 곡·앨범·플리 | ✅ | 권장 |
| 21 | GET | `/api/users/{userId}/reviews` | 유저의 리뷰 목록 + 별점 분포 | ✅ | 권장 |
| 22 | GET | `/api/curators/ranking` | 플리 주인별 평균 점수 순위 | ✅ | 추가 |

**앱이 Spotify를 직접 호출하는 기능**: 최근 재생곡, 내 Spotify 플리 목록, 플리 팔로우, 리뷰 기반 플리 생성은 사용자 본인의 Spotify 계정이 필요합니다. 앱이 PKCE로 Spotify에 직접 로그인(설정 > Spotify 연동)해서 호출하고, 이 서버는 관여하지 않습니다.

---

## 3. 공통 응답 객체

### 3.1 `UserResponse`
```json
{
  "id": 1,
  "email": "me@example.com",
  "nickname": "음악덕후",
  "profileImageUrl": null,
  "createdAt": "2026-11-10T14:30:00+09:00"
}
```
- `email`은 `GET/PATCH /api/users/me`와 회원가입·로그인 응답에만 들어갑니다. 다른 사람 정보(리뷰 작성자 등)에는 없습니다.

### 3.2 `ReviewResponse`
```json
{
  "id": 10,
  "type": "PLAYLIST",
  "spotifyId": "37i9dQZF1DXcBWIGoYBM5M",
  "item": {
    "name": "새벽 드라이브",
    "subtitle": "smedjan",
    "imageUrl": "https://i.scdn.co/image/ab67..."
  },
  "rating": 4.5,
  "comment": "곡 순서가 자연스럽다",
  "tags": ["드라이브", "새벽"],
  "playlistScores": {
    "concept": 5.0,
    "flow": 4.5,
    "discovery": 3.5,
    "snapshotId": "MTY4NjY2...",
    "ownerSpotifyId": "smedjan"
  },
  "author": { "id": 1, "nickname": "음악덕후", "profileImageUrl": null },
  "isMine": true,
  "createdAt": "2026-11-10T14:30:00+09:00",
  "updatedAt": "2026-11-10T14:30:00+09:00"
}
```
- `item`: 리뷰 작성 시 앱이 보낸 표시용 정보. 피드·프로필에서 Spotify를 다시 호출하지 않고 바로 보여 주기 위해 저장합니다.
  - `subtitle`: 곡·앨범이면 아티스트, 플리면 주인 표시 이름
- `playlistScores`: `type`이 `PLAYLIST`일 때만 값이 있고, 곡·앨범이면 `null`
- `tags`: 가나다순
- `isMine`: 요청한 사용자가 작성자이면 `true`

---

## 4. API 상세

### 0. 서버 동작 확인 — `GET /api/hello`
인증: ❌

**200 OK**
```json
{ "message": "hello", "time": "2026-11-10T14:30:00+09:00" }
```

---

### 1. 회원가입 — `POST /api/auth/signup`
인증: ❌

**Request**
```json
{ "email": "me@example.com", "password": "music1234", "nickname": "음악덕후" }
```
| 필드 | 타입 | 필수 | 규칙 |
|---|---|---|---|
| email | String | ✅ | 이메일 형식, 최대 100자. 앞뒤 공백 제거, 소문자로 저장 |
| password | String | ✅ | 8~20자, 영문과 숫자를 모두 포함 |
| nickname | String | ✅ | 앞뒤 공백 제거 후 2~20자, 중복 허용 |

**201 Created** — 가입과 동시에 로그인됩니다.
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 604800,
  "user": { "...": "UserResponse" }
}
```
**에러**: 400 `INVALID_INPUT`, 409 `EMAIL_ALREADY_EXISTS`

---

### 2. 로그인 — `POST /api/auth/login`
인증: ❌

**Request**
```json
{ "email": "me@example.com", "password": "music1234" }
```
**200 OK**: 회원가입 응답과 같은 형식
**에러**: 400 `INVALID_INPUT`, 401 `LOGIN_FAILED`

---

### 3. 내 정보 — `GET /api/users/me`
인증: ✅

**200 OK**: `UserResponse`
**에러**: 404 `USER_NOT_FOUND` (탈퇴 후 남은 토큰)

---

### 4. 내 정보 수정 — `PATCH /api/users/me`
인증: ✅

보낸 필드만 바꿉니다.
```json
{ "nickname": "새닉네임", "profileImageUrl": "https://example.com/me.png" }
```
| 필드 | 규칙 |
|---|---|
| nickname | 앞뒤 공백 제거 후 2~20자 |
| profileImageUrl | 최대 500자, http(s) URL. `""`(빈 문자열)을 보내면 사진 삭제 |

**200 OK**: `UserResponse`
**에러**: 400 `INVALID_INPUT`

> 이미지 파일 업로드는 아직 없습니다. 지금은 URL만 저장합니다.

---

### 5. 비밀번호 변경 — `PATCH /api/users/me/password`
인증: ✅

```json
{ "currentPassword": "music1234", "newPassword": "newpass5678" }
```
- `newPassword`는 회원가입과 같은 규칙. 바꿔도 기존 토큰은 만료 전까지 그대로 쓸 수 있습니다.

**204 No Content**
**에러**: 400 `INVALID_INPUT` / `PASSWORD_MISMATCH`

---

### 6. 회원 탈퇴 — `DELETE /api/users/me`
인증: ✅

```json
{ "password": "music1234" }
```
- 계정과 **내가 쓴 리뷰가 모두 삭제**됩니다. 되돌릴 수 없습니다.

**204 No Content**
**에러**: 400 `INVALID_INPUT` / `PASSWORD_MISMATCH`

---

### 7~11. Spotify 조회 (서버가 대신 호출)
인증: ✅

앱은 Spotify에 로그인하지 않아도 음악 정보를 볼 수 있습니다. 서버가 자기 Spotify 앱 자격 증명(Client Credentials)으로 Spotify Web API를 호출하고, **Spotify 응답 JSON을 그대로** 돌려줍니다. 응답 필드는 [Spotify Web API 문서](https://developer.spotify.com/documentation/web-api)를 따릅니다.

| # | 서버 경로 | 쿼리 | Spotify 호출 |
|---|---|---|---|
| 7 | `GET /api/spotify/search` | `q`(필수, 1~100자), `type`(필수: `track` `album` `artist` `playlist`), `limit`(1~50, 기본 20), `offset`(0~1000, 기본 0) | `GET /v1/search` (market=KR) |
| 8 | `GET /api/spotify/tracks/{id}` | | `GET /v1/tracks/{id}` (market=KR) |
| 9 | `GET /api/spotify/albums/{id}` | | `GET /v1/albums/{id}` (market=KR) |
| 10 | `GET /api/spotify/artists/{id}/albums` | `limit`(1~50, 기본 20), `offset`(기본 0) | `GET /v1/artists/{id}/albums` |
| 11 | `GET /api/spotify/playlists/{id}` | | `GET /v1/playlists/{id}` (market=KR) |

- `{id}`는 영문·숫자 1~64자만 허용합니다. 플리 공유 링크(`https://open.spotify.com/playlist/{id}?si=...`)에서 `{id}`를 꺼내는 건 앱이 합니다.
- 비공개 플리나 Spotify 공식(에디토리얼) 플리는 `404 SPOTIFY_NOT_FOUND`가 나올 수 있습니다.

**에러**: 400 `INVALID_INPUT`, 404 `SPOTIFY_NOT_FOUND`, 429 `SPOTIFY_RATE_LIMITED`, 502 `SPOTIFY_UNAVAILABLE`, 503 `SPOTIFY_NOT_CONFIGURED`

---

### 12. 리뷰 작성 — `POST /api/reviews`
인증: ✅

**Request: 곡·앨범**
```json
{
  "type": "TRACK",
  "spotifyId": "4uLU6hMCjMI75M1A2tKUQC",
  "item": {
    "name": "Never Gonna Give You Up",
    "subtitle": "Rick Astley",
    "imageUrl": "https://i.scdn.co/image/ab67..."
  },
  "rating": 4.5,
  "comment": "몇 번을 들어도 좋다",
  "tags": ["신나는", "80년대"]
}
```

**Request: 플레이리스트**
```json
{
  "type": "PLAYLIST",
  "spotifyId": "37i9dQZF1DXcBWIGoYBM5M",
  "item": { "name": "새벽 드라이브", "subtitle": "smedjan", "imageUrl": "https://i.scdn.co/image/ab67..." },
  "rating": 4.5,
  "conceptScore": 5.0,
  "flowScore": 4.5,
  "discoveryScore": 3.5,
  "snapshotId": "MTY4NjY2...",
  "ownerSpotifyId": "smedjan",
  "comment": "곡 순서가 자연스럽다",
  "tags": ["드라이브"]
}
```

| 필드 | 타입 | 곡·앨범 | 플리 | 규칙 |
|---|---|---|---|---|
| type | String | ✅ | ✅ | `TRACK` / `ALBUM` / `PLAYLIST` |
| spotifyId | String | ✅ | ✅ | 영문·숫자 1~64자 |
| item.name | String | ✅ | ✅ | 1~200자 |
| item.subtitle | String | 선택 | 선택 | 최대 200자 |
| item.imageUrl | String | 선택 | 선택 | 최대 500자, http(s) URL |
| rating | Number | ✅ | ✅ | 0.5~5.0, 0.5 단위 (플리는 총점) |
| comment | String | 선택 | 선택 | 최대 200자 (한 줄 평). 빈 문자열은 없음으로 저장 |
| tags | String[] | 선택 | 선택 | 최대 5개, 각 1~20자(앞뒤 공백 제거), 중복은 하나로 합침 |
| conceptScore | Number | ❌ 보내면 400 | ✅ | 0.5~5.0, 0.5 단위 |
| flowScore | Number | ❌ 보내면 400 | ✅ | 0.5~5.0, 0.5 단위 |
| discoveryScore | Number | ❌ 보내면 400 | ✅ | 0.5~5.0, 0.5 단위 |
| snapshotId | String | ❌ 보내면 400 | 선택 | 최대 100자 (Spotify `snapshot_id`) |
| ownerSpotifyId | String | ❌ 보내면 400 | ✅ | 최대 64자 (Spotify `owner.id`) |

**201 Created**: `ReviewResponse`, 헤더 `Location: /api/reviews/{id}`
**에러**: 400 `INVALID_INPUT` / `INVALID_REVIEW_FIELDS`, 409 `REVIEW_ALREADY_EXISTS`

---

### 13. 리뷰 단건 조회 — `GET /api/reviews/{reviewId}`
인증: ✅

**200 OK**: `ReviewResponse`
**에러**: 404 `REVIEW_NOT_FOUND`

---

### 14. 리뷰 수정 — `PATCH /api/reviews/{reviewId}`
인증: ✅

보낸 필드만 바꿉니다(`null`이거나 빠진 필드는 그대로). `type`, `spotifyId`, `ownerSpotifyId`는 바꿀 수 없습니다.

```json
{
  "rating": 4.0,
  "flowScore": 4.0,
  "snapshotId": "NEWSNAP...",
  "comment": "수정 후 흐름이 조금 끊긴다",
  "tags": ["드라이브", "밤"]
}
```
| 필드 | 규칙 |
|---|---|
| rating, item | 작성 때와 같음 |
| comment | `""`(빈 문자열)을 보내면 한 줄 평 삭제 |
| tags | 보내면 **전체 교체**, `[]`이면 모두 삭제 |
| conceptScore, flowScore, discoveryScore, snapshotId | 플리 리뷰만 가능, 곡·앨범에 보내면 400 `INVALID_REVIEW_FIELDS` |

**200 OK**: `ReviewResponse`
**에러**: 400 `INVALID_INPUT` / `INVALID_REVIEW_FIELDS`, 403 `REVIEW_FORBIDDEN`, 404 `REVIEW_NOT_FOUND`

---

### 15. 리뷰 삭제 — `DELETE /api/reviews/{reviewId}`
인증: ✅

태그도 함께 삭제됩니다.

**204 No Content**
**에러**: 403 `REVIEW_FORBIDDEN`, 404 `REVIEW_NOT_FOUND`

---

### 16. 내 리뷰 일괄 조회 — `GET /api/reviews/mine`
인증: ✅ · 최근 재생곡·검색 결과에서 "이미 평가함" 표시용

| 쿼리 | 필수 | 설명 |
|---|---|---|
| type | ✅ | `TRACK` 등 (대소문자 무관) |
| spotifyIds | ✅ | 쉼표로 구분, 1~50개 |

**200 OK**: 내가 리뷰를 쓴 것만 담긴 `ReviewResponse` 배열 (없으면 `[]`)

---

### 17. 항목의 리뷰 목록 — `GET /api/items/{type}/{spotifyId}/reviews`
인증: ✅

| 쿼리 | 기본값 | 설명 |
|---|---|---|
| page, size | 0, 20 | 1.5 참고 |
| sort | `latest` | `latest`(최신순), `rating_desc`(별점 높은 순), `rating_asc`(별점 낮은 순) |

**200 OK**: `Page<ReviewResponse>`. 리뷰가 없어도 404가 아니라 빈 `content`
**에러**: 400 `INVALID_ITEM_TYPE` / `INVALID_INPUT`

---

### 18. 항목 요약 — `GET /api/items/{type}/{spotifyId}/summary`
인증: ✅

상세 화면 상단(평균 별점, 리뷰 수, 분포)과 "리뷰 쓰기 / 수정" 버튼 분기에 씁니다.

**200 OK**
```json
{
  "type": "PLAYLIST",
  "spotifyId": "37i9dQZF1DXcBWIGoYBM5M",
  "reviewCount": 12,
  "averageRating": 4.3,
  "ratingDistribution": {
    "0.5": 0, "1.0": 0, "1.5": 0, "2.0": 1, "2.5": 0,
    "3.0": 1, "3.5": 2, "4.0": 3, "4.5": 3, "5.0": 2
  },
  "playlistAverages": { "concept": 4.6, "flow": 4.1, "discovery": 3.8 },
  "myReview": null
}
```
- `averageRating`: 리뷰가 없으면 `null`
- `playlistAverages`: 플리이고 리뷰가 1개 이상일 때만 값, 그 외 `null`
- `myReview`: 내 리뷰(`ReviewResponse`), 없으면 `null`
- **플리 수정 표시**: 앱이 Spotify에서 받은 현재 `snapshot_id`를 각 리뷰의 `playlistScores.snapshotId`와 비교해서, 다르면 "수정됨"을 표시합니다.

---

### 19. 최신 리뷰 피드 — `GET /api/feed` (권장, 미구현)
`page`, `size`, `type`(선택) → `Page<ReviewResponse>` 최신순

### 20. 인기 항목 — `GET /api/feed/popular` (권장, 미구현)
`type`(선택), `days`(1~90, 기본 7), `limit`(1~50, 기본 10) → `[{ type, spotifyId, item, reviewCount, averageRating }]`, 리뷰 수 내림차순

### 21. 유저의 리뷰 — `GET /api/users/{userId}/reviews` (권장, 미구현)
`page`, `size`, `type`, `minRating` → `{ user, stats: { reviewCount, averageRating, ratingDistribution, countByType }, reviews: Page }`. `stats`는 필터와 상관없이 전체 기준

### 22. 큐레이터 랭킹 — `GET /api/curators/ranking` (추가, 미구현)
`minReviews`(기본 3), `limit`(기본 20) → `[{ rank, ownerSpotifyId, ownerName, playlistCount, reviewCount, averageRating, averageScores }]`, 평균 총점 내림차순, 동점은 같은 순위

---

## 5. 결정 사항

| # | 내용 | 이유 |
|---|---|---|
| 1 | 회원가입·로그인을 Spotify 대신 **이메일 + 비밀번호**로 직접 구현 | Spotify 계정 없이도 가입 가능, 개발 모드 25명 제한을 피함 |
| 2 | 검색·곡/앨범/플리 정보는 **서버가 Client Credentials로 대신 호출** | 앱(특히 웹)에 Client Secret을 둘 수 없음 |
| 3 | 최근 재생·내 플리·팔로우·플리 생성은 앱이 Spotify 연동(PKCE) 후 직접 호출 | 사용자 본인 Spotify 권한이 필요 |
| 4 | `reviews`에 표시용 컬럼 `item_name`, `item_subtitle`, `item_image_url` | 피드·프로필·랭킹에서 Spotify 재호출 방지 |
| 5 | 플리 `rating`은 사용자가 입력하는 총점 | 기능 명세 F07 |
| 6 | 닉네임 중복 허용 | 로그인은 이메일로 하므로 닉네임은 표시용 |
| 7 | 리프레시 토큰 없이 JWT 7일 | 개인 프로젝트 범위. 필요해지면 추가 |
| 8 | Spring Boot 3 대신 4.1.2 | start.spring.io가 Boot 3을 더 이상 제공하지 않음 |
