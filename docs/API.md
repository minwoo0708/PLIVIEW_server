# 음악·플리 리뷰 앱 — REST API 명세서

> 기준 문서: 「음악·플리 리뷰 앱 기능 명세서」(2026-11-10)
> 서버: Spring Boot 4.1 / Java 21 / MySQL 8.4
> 버전: v0.1 (구현 전 초안)

---

## 1. 공통 규칙

### 1.1 기본 정보

| 항목 | 값 |
|---|---|
| Base URL (PC) | `http://localhost:8080` |
| Base URL (안드로이드 에뮬레이터) | `http://10.0.2.2:8080` |
| Content-Type | `application/json; charset=UTF-8` |
| 날짜/시간 | ISO-8601, 한국 시간 (`2026-11-10T14:30:00+09:00`) |
| ID | 서버 리소스 id는 `Long`, Spotify 리소스 id는 `String` (예: `4uLU6hMCjMI75M1A2tKUQC`) |

### 1.2 인증

- `인증` 열이 ✅인 API는 헤더가 필요합니다.
  ```
  Authorization: Bearer <서버가 발급한 JWT>
  ```
- 서버 JWT와 Spotify 토큰은 **다릅니다.** Spotify API(검색, 곡 정보 등)는 앱이 Spotify 토큰으로 직접 호출하고, 이 서버에는 서버 JWT만 보냅니다.
- JWT: HS256, 유효기간 7일, `sub` = 서버 user id.
- 토큰이 없거나 만료·위조: `401 UNAUTHORIZED`. 앱은 Spotify 토큰을 갱신한 뒤 `POST /api/auth/spotify`로 다시 로그인합니다.

### 1.3 리뷰 대상 타입 (`type`)

| 값 | 의미 | 경로 변수 표기 |
|---|---|---|
| `TRACK` | 곡 | `track` |
| `ALBUM` | 앨범 | `album` |
| `PLAYLIST` | 플레이리스트 | `playlist` |

- JSON 본문과 응답에서는 **대문자**(`TRACK`), URL 경로에서는 **소문자**(`/api/items/track/...`)를 씁니다. 서버는 경로의 대소문자를 구분하지 않습니다.

### 1.4 점수 규칙

- 모든 점수(`rating`, `conceptScore`, `flowScore`, `discoveryScore`)는 **0.5 ~ 5.0, 0.5 단위**입니다.
- 플레이리스트의 `rating`은 사용자가 직접 매기는 **총점**입니다. 세 항목의 평균으로 자동 계산하지 않습니다.
- 평균값 응답은 소수 둘째 자리에서 반올림한 **소수 첫째 자리**입니다(예: `4.3`). 리뷰가 없으면 `null`.

### 1.5 페이지네이션

요청 쿼리: `?page=0&size=20` (`page`는 0부터, `size` 기본 20, 최대 50)

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
    { "field": "rating", "reason": "0.5 단위로 입력해야 합니다." }
  ],
  "timestamp": "2026-11-10T14:30:00+09:00"
}
```
`errors`는 검증 실패(400)일 때만 들어가고, 그 외에는 빈 배열입니다.

| HTTP | code | 상황 |
|---|---|---|
| 400 | `INVALID_INPUT` | 필수값 누락, 범위·형식 오류 |
| 400 | `INVALID_REVIEW_FIELDS` | 곡/앨범 리뷰에 플리 전용 필드를 보냄, 또는 플리 리뷰에 필수 항목 누락 |
| 400 | `INVALID_ITEM_TYPE` | 경로의 `{type}`이 track/album/playlist가 아님 |
| 401 | `UNAUTHORIZED` | JWT 없음, 만료, 위조 |
| 401 | `SPOTIFY_TOKEN_INVALID` | 로그인 시 보낸 Spotify 토큰이 유효하지 않음 |
| 403 | `REVIEW_FORBIDDEN` | 남의 리뷰를 수정·삭제하려 함 |
| 404 | `USER_NOT_FOUND` | 없는 사용자 |
| 404 | `REVIEW_NOT_FOUND` | 없는 리뷰 |
| 409 | `REVIEW_ALREADY_EXISTS` | 같은 항목에 이미 리뷰를 씀 (항목당 1개) |
| 502 | `SPOTIFY_UNAVAILABLE` | Spotify 서버 오류·시간 초과 |
| 500 | `INTERNAL_ERROR` | 그 외 서버 오류 |

---

## 2. API 목록

| # | 메서드 | 경로 | 설명 | 인증 | 기능 | 우선순위 |
|---|---|---|---|---|---|---|
| 0 | GET | `/api/hello` | 서버 동작 확인 | ❌ | - | 필수 |
| 1 | POST | `/api/auth/spotify` | Spotify 토큰으로 로그인, JWT 발급 | ❌ | F01 | 필수 |
| 2 | GET | `/api/users/me` | 내 정보 조회 | ✅ | F01, F09 | 필수 |
| 3 | PATCH | `/api/users/me` | 닉네임 수정 | ✅ | F01 | 필수 |
| 4 | POST | `/api/reviews` | 리뷰 작성 | ✅ | F06, F07 | 필수 |
| 5 | GET | `/api/reviews/{reviewId}` | 리뷰 단건 조회 | ✅ | F06, F07 | 필수 |
| 6 | PATCH | `/api/reviews/{reviewId}` | 본인 리뷰 수정 | ✅ | F06, F07 | 필수 |
| 7 | DELETE | `/api/reviews/{reviewId}` | 본인 리뷰 삭제 | ✅ | F06, F07 | 필수 |
| 8 | GET | `/api/items/{type}/{spotifyId}/reviews` | 항목의 리뷰 목록 | ✅ | F04 | 필수 |
| 9 | GET | `/api/items/{type}/{spotifyId}/summary` | 평균 별점, 리뷰 수, 내 리뷰 | ✅ | F04, F07, F11 | 필수 |
| 10 | GET | `/api/feed` | 최신 리뷰 피드 | ✅ | F08 | 권장 |
| 11 | GET | `/api/feed/popular` | 리뷰 많은 곡·앨범·플리 | ✅ | F08 | 권장 |
| 12 | GET | `/api/users/{userId}/reviews` | 유저의 리뷰 목록 + 별점 분포 | ✅ | F09, F14 | 권장 |
| 13 | GET | `/api/reviews/mine` | 여러 항목에 대한 내 리뷰 일괄 조회 | ✅ | F10 | 권장 |
| 14 | GET | `/api/curators/ranking` | 플리 주인별 평균 점수 순위 | ✅ | F12 | 추가 |

**서버 API가 없는 기능**: F02(PKCE 로그인), F03(검색), F05(플리 불러오기), F10(재생 기록 조회), F13(플리 팔로우), F14(플리 생성)은 앱이 Spotify Web API를 직접 호출합니다. 서버는 리뷰 데이터만 담당합니다.

---

## 3. 공통 응답 객체

### 3.1 `UserResponse`
```json
{
  "id": 1,
  "spotifyUserId": "smedjan",
  "nickname": "음악덕후",
  "profileImageUrl": "https://i.scdn.co/image/ab67...",
  "createdAt": "2026-11-10T14:30:00+09:00"
}
```

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
  "author": { "id": 1, "nickname": "음악덕후", "profileImageUrl": "https://..." },
  "isMine": true,
  "createdAt": "2026-11-10T14:30:00+09:00",
  "updatedAt": "2026-11-10T14:30:00+09:00"
}
```
- `item`: 리뷰 작성 시 앱이 보낸 표시용 정보입니다. 피드나 프로필에서 Spotify를 다시 호출하지 않고 바로 보여 주기 위해 저장합니다(4.1 참고).
  - `subtitle`: 곡이면 아티스트, 앨범이면 아티스트, 플리면 주인 표시 이름
- `playlistScores`: `type`이 `PLAYLIST`일 때만 값이 있고, 곡·앨범이면 `null`입니다.
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

### 1. 로그인 — `POST /api/auth/spotify`
인증: ❌ · 기능: F01

앱이 PKCE로 받은 **Spotify Access Token**을 보내면, 서버가 Spotify `GET /v1/me`로 사용자를 확인합니다. 처음 로그인한 사용자면 회원을 만들고 서버 JWT를 발급합니다.

**Request**
```json
{ "spotifyAccessToken": "BQDx...." }
```
| 필드 | 타입 | 필수 | 규칙 |
|---|---|---|---|
| spotifyAccessToken | String | ✅ | 공백 불가 |

**200 OK**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 604800,
  "isNewUser": true,
  "user": { "...": "UserResponse" }
}
```
- `isNewUser`가 `true`면 앱이 닉네임 설정 화면으로 이동하고 `PATCH /api/users/me`를 호출합니다.
- 최초 닉네임은 Spotify `display_name`(20자에서 자름)입니다. 없으면 `user` + 랜덤 6자리를 씁니다.
- 다시 로그인하면 `profileImageUrl`을 Spotify 최신 값으로 갱신합니다. 닉네임은 바꾸지 않습니다.
- 서버는 Spotify 토큰을 **저장하지 않습니다.**

**에러**: 400 `INVALID_INPUT`, 401 `SPOTIFY_TOKEN_INVALID`, 502 `SPOTIFY_UNAVAILABLE`

---

### 2. 내 정보 조회 — `GET /api/users/me`
인증: ✅

**200 OK**: `UserResponse`

---

### 3. 닉네임 수정 — `PATCH /api/users/me`
인증: ✅ · 기능: F01

**Request**
```json
{ "nickname": "음악덕후" }
```
| 필드 | 타입 | 필수 | 규칙 |
|---|---|---|---|
| nickname | String | ✅ | 앞뒤 공백 제거 후 2~20자, 중복 허용 |

**200 OK**: `UserResponse`
**에러**: 400 `INVALID_INPUT`

---

### 4. 리뷰 작성 — `POST /api/reviews`
인증: ✅ · 기능: F06, F07

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
  "item": {
    "name": "새벽 드라이브",
    "subtitle": "smedjan",
    "imageUrl": "https://i.scdn.co/image/ab67..."
  },
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
| spotifyId | String | ✅ | ✅ | 1~64자, 영문·숫자 |
| item.name | String | ✅ | ✅ | 1~200자 |
| item.subtitle | String | 선택 | 선택 | 최대 200자 |
| item.imageUrl | String | 선택 | 선택 | 최대 500자, http(s) URL |
| rating | Number | ✅ | ✅ | 0.5~5.0, 0.5 단위 (플리는 총점) |
| comment | String | 선택 | 선택 | 최대 200자 (한 줄 평) |
| tags | String[] | 선택 | 선택 | 최대 5개, 각 1~20자, 중복은 하나로 합침 |
| conceptScore | Number | ❌ 보내면 400 | ✅ | 0.5~5.0, 0.5 단위 |
| flowScore | Number | ❌ 보내면 400 | ✅ | 0.5~5.0, 0.5 단위 |
| discoveryScore | Number | ❌ 보내면 400 | ✅ | 0.5~5.0, 0.5 단위 |
| snapshotId | String | ❌ 보내면 400 | ✅ | 최대 100자 (Spotify `snapshot_id`) |
| ownerSpotifyId | String | ❌ 보내면 400 | ✅ | 최대 64자 (Spotify `owner.id`) |

**201 Created**: `ReviewResponse`, 헤더 `Location: /api/reviews/{id}`

**에러**: 400 `INVALID_INPUT` / `INVALID_REVIEW_FIELDS`, 409 `REVIEW_ALREADY_EXISTS`

> 서버는 `spotifyId`가 Spotify에 실제로 있는지 확인하지 않습니다. 앱이 Spotify에서 받은 값을 그대로 보낸다고 가정합니다.

---

### 5. 리뷰 단건 조회 — `GET /api/reviews/{reviewId}`
인증: ✅

**200 OK**: `ReviewResponse`
**에러**: 404 `REVIEW_NOT_FOUND`

---

### 6. 리뷰 수정 — `PATCH /api/reviews/{reviewId}`
인증: ✅ · 기능: F06, F07

보낸 필드만 바꿉니다. `type`, `spotifyId`, `ownerSpotifyId`는 바꿀 수 없습니다.

**Request** (예: 플리 리뷰를 다시 평가)
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
| rating, comment | 작성 때와 같음 |
| tags | 보내면 **전체 교체**, `[]`이면 모두 삭제 |
| item | 보내면 표시용 정보를 갱신 |
| conceptScore, flowScore, discoveryScore, snapshotId | 플리 리뷰만 가능, 곡·앨범에 보내면 400 |
| comment를 `null`로 | 한 줄 평 삭제 |

**200 OK**: `ReviewResponse`
**에러**: 400 `INVALID_INPUT` / `INVALID_REVIEW_FIELDS`, 403 `REVIEW_FORBIDDEN`, 404 `REVIEW_NOT_FOUND`

---

### 7. 리뷰 삭제 — `DELETE /api/reviews/{reviewId}`
인증: ✅

태그도 함께 삭제됩니다.

**204 No Content**
**에러**: 403 `REVIEW_FORBIDDEN`, 404 `REVIEW_NOT_FOUND`

---

### 8. 항목의 리뷰 목록 — `GET /api/items/{type}/{spotifyId}/reviews`
인증: ✅ · 기능: F04

| 쿼리 | 기본값 | 설명 |
|---|---|---|
| page, size | 0, 20 | 1.5 참고 |
| sort | `latest` | `latest`(최신순), `rating_desc`(별점 높은 순), `rating_asc`(별점 낮은 순) |

**200 OK**: `Page<ReviewResponse>`
**에러**: 400 `INVALID_ITEM_TYPE`

리뷰가 없어도 404가 아니라 빈 `content`를 돌려줍니다.

---

### 9. 항목 요약 — `GET /api/items/{type}/{spotifyId}/summary`
인증: ✅ · 기능: F04, F07, F11

상세 화면 상단(평균 별점, 리뷰 수)과 "내 리뷰 쓰기/수정" 버튼 분기에 씁니다.

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
  "myReview": { "...": "ReviewResponse 또는 null" }
}
```
- `playlistAverages`: 플리만 값이 있고, 곡·앨범이면 `null`
- `myReview`: 내가 쓴 리뷰가 없으면 `null`
- **F11 플리 수정 표시**: 앱이 Spotify에서 받은 현재 `snapshot_id`를 각 리뷰의 `playlistScores.snapshotId`와 비교해서, 다르면 그 리뷰에 "수정됨"을 표시합니다. 서버는 비교하지 않습니다.

---

### 10. 최신 리뷰 피드 — `GET /api/feed`
인증: ✅ · 기능: F08

| 쿼리 | 기본값 | 설명 |
|---|---|---|
| page, size | 0, 20 | |
| type | 전체 | `TRACK` / `ALBUM` / `PLAYLIST`로 거르기 |

**200 OK**: `Page<ReviewResponse>` (최신순)

---

### 11. 인기 항목 — `GET /api/feed/popular`
인증: ✅ · 기능: F08 (리뷰 많은 곡·플리)

| 쿼리 | 기본값 | 설명 |
|---|---|---|
| type | 전체 | 타입 거르기 |
| days | 7 | 최근 N일 동안 작성된 리뷰 기준 (1~90) |
| limit | 10 | 1~50 |

**200 OK**
```json
[
  {
    "type": "TRACK",
    "spotifyId": "4uLU6hMCjMI75M1A2tKUQC",
    "item": { "name": "...", "subtitle": "...", "imageUrl": "..." },
    "reviewCount": 8,
    "averageRating": 4.4
  }
]
```
정렬: `reviewCount` 내림차순, 같으면 `averageRating` 내림차순. `item`은 그 항목의 가장 최근 리뷰에 저장된 값을 씁니다.

---

### 12. 유저의 리뷰 — `GET /api/users/{userId}/reviews`
인증: ✅ · 기능: F09, F14

| 쿼리 | 기본값 | 설명 |
|---|---|---|
| page, size | 0, 20 | |
| type | 전체 | 타입 거르기 |
| minRating | 없음 | 이 점수 이상만 (F14: `type=TRACK&minRating=4.5`) |

**200 OK**
```json
{
  "user": { "...": "UserResponse" },
  "stats": {
    "reviewCount": 57,
    "averageRating": 3.9,
    "ratingDistribution": { "0.5": 0, "1.0": 1, "...": 0, "5.0": 9 },
    "countByType": { "TRACK": 40, "ALBUM": 10, "PLAYLIST": 7 }
  },
  "reviews": { "...": "Page<ReviewResponse>" }
}
```
- `stats`는 `type`과 `minRating` 필터와 상관없이 그 유저의 **전체** 리뷰 기준입니다.
- 내 프로필에서는 `GET /api/users/me`로 받은 `id`를 넣어서 호출합니다.

**에러**: 404 `USER_NOT_FOUND`

---

### 13. 내 리뷰 일괄 조회 — `GET /api/reviews/mine`
인증: ✅ · 기능: F10 (최근 재생곡 중 이미 평가한 곡 표시)

| 쿼리 | 필수 | 설명 |
|---|---|---|
| type | ✅ | `TRACK` 등 |
| spotifyIds | ✅ | 쉼표로 구분, 최대 50개 |

**200 OK**: 내가 리뷰를 쓴 것만 담긴 배열
```json
[ { "...": "ReviewResponse" } ]
```

---

### 14. 큐레이터 랭킹 — `GET /api/curators/ranking`
인증: ✅ · 기능: F12

플리 리뷰를 `ownerSpotifyId`(플리 주인)별로 묶어 평균 총점(`rating`) 순으로 정렬합니다.

| 쿼리 | 기본값 | 설명 |
|---|---|---|
| minReviews | 3 | 받은 리뷰가 이 수 이상인 주인만 포함 (1~100) |
| limit | 20 | 1~100 |

**200 OK**
```json
[
  {
    "rank": 1,
    "ownerSpotifyId": "smedjan",
    "ownerName": "smedjan",
    "playlistCount": 4,
    "reviewCount": 23,
    "averageRating": 4.6,
    "averageScores": { "concept": 4.7, "flow": 4.5, "discovery": 4.2 }
  }
]
```
- `ownerName`: 그 주인의 가장 최근 플리 리뷰에 저장된 `item.subtitle`
- 평균이 같으면 같은 `rank`를 줍니다(1, 1, 3 방식).

---

## 5. 기능 명세서와 달라진 점 / 결정 사항

| # | 내용 | 이유 |
|---|---|---|
| 1 | `reviews`에 표시용 컬럼 `item_name`, `item_subtitle`, `item_image_url`을 추가 | 피드, 프로필, 랭킹에서 항목마다 Spotify를 다시 호출하지 않기 위해서 |
| 2 | `GET /api/hello`, `GET /api/reviews/{id}`, `GET /api/feed/popular`, `GET /api/reviews/mine`을 추가 | 2단계 서버 확인, 수정 화면, F08 인기 항목, F10 일괄 확인 |
| 3 | 플리 `rating`은 사용자가 입력하는 총점 | 기능 명세 F07의 "총점" 항목 |
| 4 | 닉네임 중복 허용 | 기능 명세에 UNIQUE 제약이 없음 |
| 5 | Spring Boot 3 대신 **4.1.2** | start.spring.io가 지금은 Boot 3을 제공하지 않음 |
| 6 | 별점 분포는 0.5 단위 10칸 | 별점이 0.5 단위이기 때문 |
