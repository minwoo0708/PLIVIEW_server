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

