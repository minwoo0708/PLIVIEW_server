-- PLIVIEW MySQL 8.4 스키마 v3 (docs/API.md 기준, 이메일 로그인 + Spotify 연동 + 리뷰 하이라이트)
-- v2로 만든 DB를 v3로 올리려면 docs/migrations/V3__review_highlights_and_spotify_link.sql
-- 실행 (PowerShell): mysql -u root -p -e "source docs/schema.sql"
-- 실행 (cmd/bash):   mysql -u root -p < docs/schema.sql
--
-- v1(Spotify 로그인)으로 이미 만든 DB가 있다면, 데이터가 없을 때 아래 한 줄로 지우고 다시 실행:
--   DROP DATABASE music_review;

CREATE DATABASE IF NOT EXISTS music_review
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE music_review;

CREATE TABLE IF NOT EXISTS users (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    email             VARCHAR(100) NOT NULL,              -- 소문자로 저장
    password_hash     VARCHAR(100) NOT NULL,              -- BCrypt
    nickname          VARCHAR(20)  NOT NULL,
    profile_image_url VARCHAR(500) NULL,
    spotify_user_id      VARCHAR(64)  NULL,               -- 연동한 Spotify 계정 id (플리 owner.id)
    spotify_display_name VARCHAR(100) NULL,
    created_at        DATETIME(6)  NOT NULL,
    updated_at        DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT uk_users_spotify_user_id UNIQUE (spotify_user_id)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS reviews (
    id               BIGINT        NOT NULL AUTO_INCREMENT,
    user_id          BIGINT        NOT NULL,
    type             VARCHAR(10)   NOT NULL,              -- TRACK / ALBUM / PLAYLIST
    spotify_id       VARCHAR(64)   NOT NULL,

    -- 표시용 정보 (작성 시 앱이 전달)
    item_name        VARCHAR(200)  NOT NULL,
    item_subtitle    VARCHAR(200)  NULL,                  -- 아티스트 또는 플리 주인 이름
    item_image_url   VARCHAR(500)  NULL,

    rating           DECIMAL(2, 1) NOT NULL,              -- 0.5 ~ 5.0 (플리는 총점)
    comment          VARCHAR(200)  NULL,

    -- 플레이리스트 전용 (곡·앨범은 NULL)
    concept_score    DECIMAL(2, 1) NULL,
    flow_score       DECIMAL(2, 1) NULL,
    discovery_score  DECIMAL(2, 1) NULL,
    snapshot_id      VARCHAR(100)  NULL,
    owner_spotify_id VARCHAR(64)   NULL,

    -- 앨범·플리 하이라이트 (곡은 NULL, 과소평가된 곡은 앨범만)
    best_track_id         VARCHAR(64)   NULL,
    best_track_name       VARCHAR(200)  NULL,
    worst_track_id        VARCHAR(64)   NULL,
    worst_track_name      VARCHAR(200)  NULL,
    underrated_track_id   VARCHAR(64)   NULL,
    underrated_track_name VARCHAR(200)  NULL,
    best_moment           VARCHAR(100)  NULL,
    body                  VARCHAR(2000) NULL,             -- 전체 평가 (여러 줄)

    created_at       DATETIME(6)   NOT NULL,
    updated_at       DATETIME(6)   NOT NULL,

    PRIMARY KEY (id),
    CONSTRAINT fk_reviews_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uk_reviews_user_item UNIQUE (user_id, type, spotify_id),
    CONSTRAINT ck_reviews_type CHECK (type IN ('TRACK', 'ALBUM', 'PLAYLIST')),
    CONSTRAINT ck_reviews_rating CHECK (rating BETWEEN 0.5 AND 5.0 AND MOD(rating * 10, 5) = 0),
    CONSTRAINT ck_reviews_playlist_fields CHECK (
        (type = 'PLAYLIST' AND concept_score IS NOT NULL AND flow_score IS NOT NULL
             AND discovery_score IS NOT NULL AND owner_spotify_id IS NOT NULL)
        OR
        (type <> 'PLAYLIST' AND concept_score IS NULL AND flow_score IS NULL
             AND discovery_score IS NULL AND snapshot_id IS NULL AND owner_spotify_id IS NULL)
    ),

    INDEX idx_reviews_item (type, spotify_id, created_at),   -- 항목 상세의 리뷰 목록, 평균
    INDEX idx_reviews_created (created_at),                  -- 피드 최신순, 인기 항목(최근 N일)
    INDEX idx_reviews_user_created (user_id, created_at),    -- 유저 프로필
    INDEX idx_reviews_owner (owner_spotify_id)               -- 큐레이터 랭킹
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS review_tags (
    review_id BIGINT      NOT NULL,
    tag       VARCHAR(20) NOT NULL,
    PRIMARY KEY (review_id, tag),
    CONSTRAINT fk_review_tags_review FOREIGN KEY (review_id) REFERENCES reviews (id) ON DELETE CASCADE,
    INDEX idx_review_tags_tag (tag)
) ENGINE = InnoDB;
