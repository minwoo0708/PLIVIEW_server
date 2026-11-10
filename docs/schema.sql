-- 음악·플리 리뷰 앱 MySQL 8.4 스키마 (docs/API.md 기준)
-- 실행: mysql -u root -p < docs/schema.sql

CREATE DATABASE IF NOT EXISTS music_review
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE music_review;

CREATE TABLE IF NOT EXISTS users (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    spotify_user_id   VARCHAR(64)  NOT NULL,
    nickname          VARCHAR(20)  NOT NULL,
    profile_image_url VARCHAR(500) NULL,
    created_at        DATETIME(6)  NOT NULL,
    updated_at        DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
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
