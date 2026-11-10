# PLIVIEW Flutter 개발 인수인계서

> 작성일: 2026-11-10 · 대상: PLIVIEW 앱(Flutter) 개발 담당
> 이 문서 하나로 앱 개발을 시작할 수 있도록 서버, 디자인, 규칙을 모두 정리했습니다.

---

> **2026-11-10 추가 (서버 v0.4)**: 앱에서 요청한 서버 기능이 구현됐습니다. 상세는 `docs/API.md` 23~28번과 12·14·21·22번 변경 부분을 보세요.
> - 앨범·플리 리뷰 하이라이트: `bestTrack`, `worstTrack`, `underratedTrack`(앨범만), `bestMoment`, `body`(여러 줄) → 응답 `highlights`
> - 수록곡 곡별 평점: `GET /api/items/tracks/ratings?spotifyIds=a,b,c` (최대 100개, `myRating` 포함)
> - Spotify 계정 연동: `PUT /api/users/me/spotify {spotifyAccessToken}` / `DELETE /api/users/me/spotify`, `UserResponse.spotify`로 연동 여부 확인
> - 베스트 큐레이터: Spotify 연동한 사용자만, 응답에 `userId`·`nickname` 추가 → `GET /api/curators/{userId}`, `GET /api/curators/{userId}/playlists`(인기순)
> - 이번 주의 플리: `GET /api/feed/weekly-playlists` (기본 리뷰 50개 이상, 평균 높은 순)
> - 별 5점 모아 보기: `GET /api/users/{id}/reviews?type=TRACK|ALBUM|PLAYLIST&minRating=5.0`

## 0. 한눈에 보기

| 항목 | 내용 |
|---|---|
| 앱 | **PLIVIEW**: Spotify의 곡·앨범·플레이리스트에 별점과 한 줄 평을 남기는 앱 |
| 플랫폼 | **Flutter 하나로 안드로이드 앱 + 웹** (Windows 데스크톱은 안 함) |
| 개발 환경 | Flutter 3.47.9 stable, Android SDK 36, Chrome. `C:\dev\flutter`에 설치돼 있음 (`flutter doctor` 통과, Visual Studio만 없음 = 문제없음) |
| 디자인 | Figma 페이지 **"PLIVIEW v2"** (아래 3장) |
| 서버 | Spring Boot 서버 **구현 완료**, 로컬 `http://localhost:8080` (아래 2장) |
| 서버 코드 | `C:\Users\USER\Documents\back` · GitHub `minwoo0708/PLIVIEW_server` |
| API 명세 원본 | `C:\Users\USER\Documents\back\docs\API.md` (**요청·응답 상세는 반드시 이 파일 기준**) |
| 앱 코드 위치 (새로 만들 것) | `C:\Users\USER\Documents\front` · GitHub 새 레포 `minwoo0708/PLIVIEW_app` |

---

## 1. 작업 규칙 (꼭 지킬 것)

1. **서버 폴더(`Documents\back`)는 읽기만** 합니다. 서버 코드를 고쳐야 하면 고치지 말고 사용자에게 알립니다.
2. 앱은 **새 폴더 `C:\Users\USER\Documents\front`**에 만들고, **새 GitHub 레포 `PLIVIEW_app`**(공개 여부는 사용자에게 확인)을 씁니다.
3. **커밋을 기능 단위로 잘게, 많이** 나눕니다(사용자가 원함). 빈 커밋이나 의미 없는 커밋은 금지합니다.
4. 커밋 메시지·PR 본문에 **Claude 흔적을 남기지 않습니다.** `Co-Authored-By: Claude`, "Generated with Claude Code" 같은 줄은 금지입니다. 작성자는 사용자 git 계정(`minwoo0708`, noreply 이메일)입니다.
5. 기능마다 브랜치(`feature/...`) → PR → 사용자가 머지하는 흐름입니다.
6. 비밀값(Spotify Client Secret, 사용자 비밀번호 등)은 앱 코드에 **절대 넣지 않습니다.** Spotify Client ID는 공개값이라 넣어도 됩니다.
7. 사용자는 한국어로 쉽게 설명받는 걸 선호합니다.

---

## 2. 서버 연결

### 2.1 서버 실행 (사용자 PC)
- IntelliJ에서 `MusicReviewServerApplication` ▶ 실행. 환경 변수(MySQL, Spotify 키)는 이미 설정돼 있습니다.
- 확인: `GET http://localhost:8080/api/hello` → `{"message":"hello", ...}`
- DB: MySQL 8.4 (`music_review`). 데이터가 유지됩니다.

### 2.2 Base URL
| 실행 환경 | Base URL |
|---|---|
| Flutter 웹 (Chrome) | `http://localhost:8080` (서버가 `localhost:*` CORS 허용) |
| 안드로이드 에뮬레이터 | `http://10.0.2.2:8080` |
| 실제 폰 (같은 Wi-Fi) | `http://<PC IP>:8080` |

→ `kIsWeb` / 플랫폼으로 자동 선택하고, `--dart-define=API_BASE_URL=...`로 덮어쓸 수 있게 만드세요.
→ 안드로이드는 http(평문)라서 `android:usesCleartextTraffic="true"` 또는 network security config가 필요합니다.

### 2.3 인증
- 이메일 + 비밀번호로 가입·로그인 → 응답의 `accessToken`(JWT, 7일)을 저장합니다.
- 이후 모든 요청 헤더: `Authorization: Bearer <accessToken>`
- 공개 API(토큰 불필요): `GET /api/hello`, `POST /api/auth/signup`, `POST /api/auth/login`
- **401 `UNAUTHORIZED`** → 토큰을 지우고 로그인 화면으로 보냅니다. 리프레시 토큰은 없습니다.
- **404 `USER_NOT_FOUND`** on `/api/users/me` → 탈퇴한 계정이므로 토큰을 지우고 로그인 화면으로 보냅니다.
- 로그아웃 = 저장한 토큰만 삭제(서버 호출 없음)
- 앱 시작 시: 토큰이 있으면 `GET /api/users/me`로 유효성 확인 → 홈, 없거나 실패하면 → 시작/로그인

### 2.4 공통 응답 형식
**에러** (모든 4xx/5xx):
```json
{ "status": 400, "code": "INVALID_INPUT", "message": "입력값이 올바르지 않습니다.",
  "errors": [ { "field": "rating", "reason": "0.5부터 5.0까지 0.5 단위로 입력해야 합니다." } ],
  "timestamp": "2026-11-10T14:30:00+09:00" }
```
- `message`는 한국어라 사용자에게 그대로 보여 줘도 됩니다. 검증 실패는 `errors[].field`로 해당 입력칸 아래에 표시하세요.

**페이지** (`?page=0&size=20`, size 최대 50):
```json
{ "content": [ ... ], "page": 0, "size": 20, "totalElements": 134, "totalPages": 7, "hasNext": true }
```
→ 무한 스크롤은 `hasNext`가 false가 될 때까지 `page+1`로 요청합니다.

**시간**: ISO-8601 `+09:00` (예: `2026-11-10T14:30:00+09:00`)

### 2.5 주요 에러 코드 (화면 처리용)
| code | HTTP | 화면 처리 |
|---|---|---|
| `INVALID_INPUT` | 400 | `errors[]`를 필드별로 표시 |
| `INVALID_REVIEW_FIELDS` | 400 | 앱 버그. 플리 전용 필드를 곡에 보냈거나 빠뜨림 |
| `PASSWORD_MISMATCH` | 400 | "현재 비밀번호가 일치하지 않습니다" |
| `LOGIN_FAILED` | 401 | "이메일 또는 비밀번호가 틀렸습니다" |
| `UNAUTHORIZED` | 401 | 로그인 화면으로 |
| `REVIEW_FORBIDDEN` | 403 | 남의 리뷰는 수정·삭제 버튼을 아예 숨김(`isMine`) |
| `REVIEW_NOT_FOUND` / `SPOTIFY_NOT_FOUND` | 404 | "찾을 수 없어요" 빈 화면 |
| `EMAIL_ALREADY_EXISTS` | 409 | 가입 이메일 칸 아래 표시 |
| `REVIEW_ALREADY_EXISTS` | 409 | 이미 리뷰가 있음 → 수정 화면으로 |
| `SPOTIFY_RATE_LIMITED` | 429 | "잠시 후 다시 시도" |
| `SPOTIFY_UNAVAILABLE` / `SPOTIFY_NOT_CONFIGURED` | 502 / 503 | "음악 정보를 불러올 수 없어요" + 다시 시도 |

---

## 3. 디자인 (Figma)

- 파일: https://www.figma.com/design/lqM2Jzs3WPJbTmEE1xJ9N2/Music%C2%B7Playlist-Review-App?node-id=9-2
- **사용할 페이지: `PLIVIEW v2` (id `9:2`)**. 다른 페이지("원본 백업", "색 통일본 백업", "Jua 적용 전 백업", "PLIVIEW App Screens")는 **이전 버전이니 쓰지 마세요.**
- 프레임 크기 390 × 844 (모바일 기준). 웹은 가운데 정렬 + 최대 너비로 같은 레이아웃을 쓰고, 넓은 화면 레이아웃은 별도 디자인이 없습니다.
- 전체 미리보기 이미지: `C:\Users\USER\Documents\back\docs\design\overview.png`
- Figma MCP로 각 화면의 상세(`get_design_context`)와 아이콘·SVG 에셋을 받을 수 있습니다. fileKey `lqM2Jzs3WPJbTmEE1xJ9N2`. 에셋 URL은 7일 뒤 만료되니 **반드시 프로젝트 `assets/`에 내려받아** 쓰세요.

### 3.1 화면 목록 (Figma node id)
| # | 화면 | node id | 쓰는 API |
|---|---|---|---|
| 01 | 시작하기 | `9:3` | (없음) → 가입/로그인 이동 |
| 02 | 홈 | `9:47` | `GET /api/feed/popular`, `GET /api/feed` |
| 03 | 탐색(검색) | `9:108` | `GET /api/spotify/search`, `GET /api/reviews/mine` |
| 04 | 재생 기록 | `9:187` | Spotify 사용자 연동 필요 (5장, 2단계) |
| 05 | 나(프로필) | `10:2` | `GET /api/users/me`, `GET /api/users/{id}/reviews` |
| 06 | 앨범 상세 | `10:76` | `GET /api/spotify/albums/{id}`, `GET /api/items/album/{id}/summary`, `.../reviews` |
| 07 | 플리 상세 | `10:144` | `GET /api/spotify/playlists/{id}`, `GET /api/items/playlist/{id}/summary`, `.../reviews` |
| 08 | 평가하기 (곡·앨범) | `10:202` | `POST /api/reviews`, `PATCH /api/reviews/{id}` |
| 09 | 평가하기 (플리) | `10:250` | `POST /api/reviews`, `PATCH /api/reviews/{id}` |
| 10 | 큐레이터 랭킹 | `10:334` | `GET /api/curators/ranking` |
| 11 | 로그인 | `23:18` | `POST /api/auth/login` |
| 12 | 회원가입 | `23:49` | `POST /api/auth/signup` |
| 13 | 설정 | `23:96` | `PATCH /api/users/me`, `PATCH /api/users/me/password`, `DELETE /api/users/me` |
| - | 컴포넌트 (로그인·가입·설정용) | `23:2` | 입력칸, 기본 버튼, 설정 행 |

곡 상세 화면은 따로 없습니다. **앨범 상세(06)를 곡에도 재사용**하세요(수록곡 목록만 숨김).

### 3.2 디자인 토큰 (Figma에서 추출)
Figma 변수(Variables)는 정의돼 있지 않아서, 화면에서 직접 뽑은 값입니다. `ThemeData`와 상수 파일로 만드세요.

**색상**
| 이름 | 값 | 용도 |
|---|---|---|
| `ink` | `#170A40` | 진한 남보라 배경, 로그인 배경, 하단 내비, 진한 버튼 글자 |
| `inkMid` | `#2A1475` | 배경 그라데이션 중간 |
| `primary` | `#5B2EFF` | 강조, 선택된 점수 버튼, "더보기" 링크 |
| `primaryLight` | `#9267F7` | 히어로 카드 그라데이션 시작 |
| `lavender` | `#C9B8FF` | 기본 버튼 배경, 배지, 선택된 탭 |
| `surface` | `#F6F2FF` | 화면 아래쪽 밝은 배경 |
| `chip` | `#F1ECFF` | 선택 안 된 점수 버튼 |
| `border` | `#E4DEF5` | 흰 카드 테두리 |
| `textPrimary` | `#1A1233` | 밝은 배경 위 제목 |
| `textSecondary` | `#6F6A8A` | 밝은 배경 위 보조 글자 |
| 흰 글자 | `white` · 85% · 75% · 70% · 55% | 어두운 배경 위 본문·보조·플레이스홀더 |
| 반투명 흰 | 16% (원형 아이콘 버튼), 14% (대상 칩), 8% + 테두리 16% (입력칸) | |
| 내비 배경 | `#170A40` 94% 불투명 | 하단 플로팅 내비 |

**그라데이션**
- 화면 배경(홈): 위→아래 `#170A40` 0% → `#2A1475` 30% → `#F6F2FF` 56% → `#F6F2FF` 100%
- 화면 배경(평가하기): 같은 색, 정지점 0 / 26 / 40 / 100%
- 히어로 카드: `#9267F7` → `#5B2EFF` (약 163°, 끝점 73%)
- 로그인·회원가입: 단색 `#170A40` + 오른쪽 위 장식 링(SVG 에셋)
- 앨범 커버 자리표시: 연한색 → 진한색 대각선 그라데이션 + 원 장식. **실제 앱에서는 Spotify 커버 이미지로 바꿉니다**

**글꼴** (모두 Google Fonts에 있음 → `google_fonts` 패키지 또는 assets 번들)
| 글꼴 | 용도 |
|---|---|
| **Jua** | 큰 제목(30~34px, 자간 -1%), 카드 제목(22px), 큰 점수 숫자 |
| **Gothic A1** | 한글 본문. Regular(본문·보조), Medium(인사말), SemiBold(라벨·버튼), ExtraBold(섹션 제목 17px, 카드 제목 14px, 버튼) |
| **Space Grotesk** | 영문·숫자. 곡명, 아티스트명, 점수, 시간. Regular/Medium/Bold |

주요 크기: 큰 제목 30(줄 높이 38) / 로그인 제목 34 / 섹션 제목 17 / 카드 제목 14 / 본문 13~15 / 보조 11~12 / 버튼 16

**모양·간격**
- 모서리: 알약형 999 (버튼, 배지, 칩, 내비), 히어로 카드 28, 흰 카드 24, 총점 바 20, 커버 18, 입력칸 16, 점수 버튼 14
- 화면 좌우 여백 20 (로그인·가입은 24), 섹션 간격 14~16, 카드 안쪽 18
- 기본 버튼: 높이 52, `lavender` 배경, `ink` 글자, Gothic A1 ExtraBold 16
- 입력칸: 라벨(SemiBold 13, 흰 75%) + 52px 필드(흰 8% 배경, 흰 16% 테두리, 모서리 16, 비밀번호는 눈 아이콘)
- 하단 플로팅 내비: 좌우 20 띄움, 폭 350, 알약형, 선택된 탭은 `lavender` 알약 + 아이콘 + 글자, 나머지는 아이콘만(투명도 65%). 탭: 홈 / 탐색 / 기록 / 나
- 점수 배지(커버 위): 검정 55% 알약 + "★ 4.2"

---

## 4. 화면별 구현 메모

### 01 시작하기 / 11 로그인 / 12 회원가입
- 가입 규칙: 이메일 형식·최대 100자 / 비밀번호 **8~20자, 영문+숫자** / 닉네임 **2~20자**(앞뒤 공백 제외, 중복 허용)
- 가입 성공(201) 응답에 토큰이 있으므로 **바로 로그인 상태로 홈 이동**
- ⚠️ 시작하기 화면의 초록 **"Spotify 계정으로 시작"** 버튼은 지금 서버 구조와 맞지 않습니다(6장 결정 필요).

### 02 홈
- 상단 히어로 카드("이주의 앨범"): 전용 API가 없습니다 → `GET /api/feed/popular?type=ALBUM&days=7&limit=1` 첫 항목을 쓰세요. 없으면 카드를 숨깁니다.
- "지금 많이 듣는 앨범" 그리드: `GET /api/feed/popular?type=ALBUM&limit=10`
- 최신 리뷰 피드(디자인에 별도 섹션이 없다면 아래에 추가): `GET /api/feed`
- 🔔 알림 버튼: 서버 기능이 없습니다 → 비활성 또는 숨김

### 03 탐색
- 검색창 + 탭 4개(곡/앨범/아티스트/플리) → `GET /api/spotify/search?q=...&type=track|album|artist|playlist&limit=20&offset=0`
- 입력이 멈추고 0.3초 뒤 자동 검색(디바운스)
- **응답은 Spotify JSON 원본**입니다. 자주 쓰는 경로:
  - 곡: `tracks.items[].{id, name, artists[].name, album.images[0].url, duration_ms, external_urls.spotify}`
  - 앨범: `albums.items[].{id, name, artists[].name, images[0].url, release_date}`
  - 아티스트: `artists.items[].{id, name, images[0].url}` → 누르면 `GET /api/spotify/artists/{id}/albums`
  - 플리: `playlists.items[]` (**null 항목이 섞일 수 있으니 거르세요**) `.{id, name, owner.id, owner.display_name, images[0].url, snapshot_id}`
  - 페이지: `{type}s.next`가 null이 아니면 `offset += limit`
- "공유 링크로 플리 불러오기": `https://open.spotify.com/playlist/{id}?si=...`에서 `{id}`(영문·숫자)를 뽑아 플리 상세로 이동
- 이미 평가한 항목 표시: 결과 id들을 `GET /api/reviews/mine?type=TRACK&spotifyIds=a,b,c`(최대 50개)로 조회해서 별점 배지 표시

### 05 나 (프로필)
- `GET /api/users/me`로 내 id → `GET /api/users/{id}/reviews`
- `stats.ratingDistribution`(키 "0.5"~"5.0", 10칸) → 막대그래프, `stats.countByType`(TRACK/ALBUM/PLAYLIST) → "리뷰 42 · 평균 ★3.8 · 플리 12" 같은 칩
- 리뷰 목록은 `reviews`(페이지)
- 오른쪽 위 ⚙ → 13 설정

### 06 앨범(곡) 상세
- 위쪽 정보: `GET /api/spotify/albums/{id}` (곡이면 `/tracks/{id}`)
- 평균·분포·내 리뷰: `GET /api/items/album/{id}/summary`
  - `myReview`가 null → **"평가하기"**, 있으면 → **"내 평가 수정"**
  - `averageRating` null → "아직 리뷰가 없어요"
- 리뷰 목록: `GET /api/items/album/{id}/reviews?sort=latest|rating_desc|rating_asc`
- "Spotify에서 듣기": `external_urls.spotify`를 `url_launcher`로 엽니다(미리듣기 없음)

### 07 플리 상세
- 정보 + 수록곡: `GET /api/spotify/playlists/{id}` (Spotify가 플리 응답 구조를 바꿔 온 적이 있으니 **수록곡 필드 경로는 실제 응답을 한 번 찍어 보고 확정**하세요. 곡 객체가 null인 항목은 거르기)
- 요약: `GET /api/items/playlist/{id}/summary` → `playlistAverages`(컨셉/흐름/발견성 평균) 카드 4개
- **"수정됨" 배지**: 각 리뷰의 `playlistScores.snapshotId` ≠ Spotify 응답의 현재 `snapshot_id`이면 표시
- 하단 "이 플리 평가하기" 버튼 (내 리뷰가 있으면 "내 평가 수정")
- 비공개·Spotify 공식 플리는 404 `SPOTIFY_NOT_FOUND`가 날 수 있습니다 → "불러올 수 없는 플리예요"

### 08 평가하기 (곡·앨범)
- 별점 **0.5 단위**(반 별), 한 줄 평(최대 200자, 카운터), 태그(최대 5개, 각 20자)
- 작성: `POST /api/reviews`
  ```json
  { "type": "ALBUM", "spotifyId": "...",
    "item": { "name": "BULLY - DELUXE", "subtitle": "Kanye West, Ye", "imageUrl": "https://i.scdn.co/..." },
    "rating": 4.5, "comment": "...", "tags": ["..."] }
  ```
  - `item`은 **Spotify에서 받은 표시용 정보를 그대로** 보냅니다(피드·프로필에서 다시 조회하지 않으려고 서버가 저장함). subtitle = 아티스트 이름들
- 수정: `PATCH /api/reviews/{id}` — 바뀐 필드만 보냅니다. `comment: ""` = 삭제, `tags: []` = 전부 삭제

### 09 평가하기 (플리)
- 디자인: 컨셉 일치도 / 곡 흐름 / 발견성을 각각 **1~5 정수 버튼**으로 고르고, **총점은 자동 계산해서 표시**합니다(입력칸 없음)
- 서버는 `rating`(총점)이 **필수이고 0.5 단위**여야 합니다 → **총점 = 세 점수 평균을 0.5 단위로 반올림**
  예: 5·4·3 → 4.0 / 5·4·4 → 4.33 → **4.5** / 3·3·4 → 3.33 → **3.5**
  공식: `(avg * 2).round() / 2`
- 요청:
  ```json
  { "type": "PLAYLIST", "spotifyId": "...",
    "item": { "name": "새벽 3시의 드라이브", "subtitle": "<owner.display_name>", "imageUrl": "..." },
    "rating": 4.0, "conceptScore": 5, "flowScore": 4, "discoveryScore": 3,
    "snapshotId": "<snapshot_id>", "ownerSpotifyId": "<owner.id>" }
  ```
  - `ownerSpotifyId` **필수**(큐레이터 랭킹용), `snapshotId`는 넣어 주세요("수정됨" 표시용)
- 하단 안내 "지금 버전의 플리(snapshot)로 저장돼요"는 그대로 둡니다

### 10 큐레이터 랭킹
- `GET /api/curators/ranking?minReviews=3&limit=20` → `rank`(동점은 같은 순위), `ownerName`, `averageRating`, `reviewCount`, `playlistCount`
- 테스트 데이터가 적을 땐 `minReviews=1`로 확인

### 13 설정
- 닉네임 변경 `PATCH /api/users/me {"nickname"}` / 프로필 사진은 **URL만** 저장 가능(업로드 API 없음 → 일단 숨기거나 기본 아바타)
- 비밀번호 변경 `PATCH /api/users/me/password {"currentPassword","newPassword"}` → 204
- Spotify 연동 행 → 5장 2단계(그 전까지는 "준비 중")
- 로그아웃 → 토큰 삭제 → 로그인 화면
- 회원 탈퇴 `DELETE /api/users/me` 본문 `{"password"}` → 204 → 토큰 삭제 → 시작 화면. "내가 쓴 리뷰도 모두 삭제됩니다" 확인창 필수

---

## 5. Spotify 관련

- 음악 정보(검색, 곡·앨범·공개 플리)는 **전부 우리 서버 `/api/spotify/**`를 거칩니다.** 앱이 Spotify에 직접 로그인할 필요가 없습니다.
- **브랜딩 의무**: Spotify 데이터를 보여 주는 화면에는 Spotify 로고/아이콘과 원본 링크(`external_urls.spotify`)를 표시하고, 커버 이미지는 자르거나 변형하지 않습니다. https://developer.spotify.com/documentation/design
- **2단계 (나중에)**: "재생 기록(04)", "내 Spotify 플리 불러오기", "플리 팔로우", "리뷰 기반 플리 생성"은 **사용자 본인 Spotify 계정**이 필요합니다 → 앱에서 PKCE로 Spotify에 직접 로그인
  - Client ID: `120ed05f019a4d0791e7e90e300256e3` (공개값, Secret은 앱에 넣지 않음)
  - 등록된 Redirect URI: `pliview://callback`(안드로이드), `http://127.0.0.1:8080/callback`(웹 테스트). `localhost`는 Spotify가 거부합니다
  - 필요한 scope: `user-read-recently-played`, `playlist-read-private`, `playlist-modify-public`, `playlist-modify-private`
  - 개발 모드라 Spotify Dashboard → User Management에 등록한 계정(최대 25명)만 연동 가능
  - 1단계(필수 기능)에서는 04 탭을 "Spotify 연동하면 볼 수 있어요" 안내 화면으로 둡니다

---

## 6. 사용자에게 먼저 확인할 것 (디자인 ↔ 서버 불일치)

1. **시작하기(01)의 "Spotify 계정으로 시작" 버튼**: 서버는 이메일 가입·로그인만 지원합니다.
   → 추천: 초록 버튼을 **"이메일로 시작하기"(회원가입)**로 바꾸고, 아래 링크는 "이미 계정이 있나요? 로그인". Spotify 연동은 설정으로 옮깁니다.
2. **플리 총점**: 위 4장 09처럼 세 점수 평균을 0.5 단위로 반올림해서 보내는 것으로 진행해도 되는지
3. **홈 히어로 "이주의 앨범"**: 인기 앨범 1위로 대체해도 되는지
4. **알림(🔔)**: 기능이 없으니 숨길지
5. 새 레포 **공개/비공개**

---

## 7. 추천 패키지·구조

| 용도 | 패키지 |
|---|---|
| 상태 관리 | `flutter_riverpod` |
| 라우팅 | `go_router` (웹 URL 지원, 로그인 여부로 redirect) |
| HTTP | `dio` (인터셉터로 토큰 헤더, 401 처리) |
| 토큰 저장 | `flutter_secure_storage` (웹에서는 localStorage 기반이라 보안이 약함을 인지) |
| 글꼴 | `google_fonts` (Jua, Gothic A1, Space Grotesk) |
| 이미지 | `cached_network_image` |
| SVG 아이콘 | `flutter_svg` |
| 외부 링크 | `url_launcher` |
| JSON 모델 | `freezed` + `json_serializable` (또는 수동) |

```
lib/
  core/        api_client, env(base url), error 처리, theme(colors, text styles), router
  features/
    auth/      start, login, signup
    home/      feed, popular
    search/    spotify 검색
    item/      album·track 상세, playlist 상세
    review/    평가하기(곡·앨범, 플리)
    profile/   나, 설정
    curator/   랭킹
  shared/      공통 위젯: 버튼, 입력칸, 리뷰 카드, 별점, 하단 내비
```

---

## 8. 작업 순서 (권장)

1. **프로젝트 생성**: `flutter create --platforms=android,web --org com.pliview pliview_app` (위치 `Documents\front`), git init, GitHub 레포 연결, 안드로이드 cleartext 설정
2. **기반**: 테마(색·글꼴·모양), 공통 위젯(기본 버튼, 입력칸, 하단 플로팅 내비), API 클라이언트, 토큰 저장, 라우터
3. **인증**: 시작 → 로그인 / 회원가입 → 홈 (서버 연동, 에러 표시까지)
4. **탐색**: Spotify 검색 4개 탭, 플리 링크 붙여넣기
5. **상세 + 평가**: 앨범·곡 상세 → 평가하기(곡·앨범) → 플리 상세 → 평가하기(플리), 수정·삭제
6. **홈·프로필·랭킹·설정**
7. **(2단계)** Spotify PKCE 연동 → 재생 기록 탭

각 단계가 끝날 때마다 **웹(Chrome)과 안드로이드 에뮬레이터 둘 다**에서 서버와 붙여 실제로 동작하는지 확인합니다.

---

## 9. 참고 자료 경로

| 자료 | 위치 |
|---|---|
| API 명세 (전체 요청·응답) | `C:\Users\USER\Documents\back\docs\API.md` |
| DB 스키마 | `C:\Users\USER\Documents\back\docs\schema.sql` |
| Postman 컬렉션 (실제 요청 예시) | `C:\Users\USER\Documents\back\postman\` |
| HTTP 요청 예시 | `C:\Users\USER\Documents\back\http\api.http` |
| 디자인 전체 미리보기 | `C:\Users\USER\Documents\back\docs\design\overview.png` |
| Figma | 위 3장 링크 (페이지 `PLIVIEW v2`) |
