-- V2: 앨범·플리 리뷰 하이라이트, Spotify 계정 연동 (API v0.4)


ALTER TABLE users
    ADD COLUMN spotify_user_id      VARCHAR(64)  NULL AFTER profile_image_url,
    ADD COLUMN spotify_display_name VARCHAR(100) NULL AFTER spotify_user_id,
    ADD CONSTRAINT uk_users_spotify_user_id UNIQUE (spotify_user_id);

ALTER TABLE reviews
    ADD COLUMN best_track_id         VARCHAR(64)   NULL AFTER owner_spotify_id,
    ADD COLUMN best_track_name       VARCHAR(200)  NULL AFTER best_track_id,
    ADD COLUMN worst_track_id        VARCHAR(64)   NULL AFTER best_track_name,
    ADD COLUMN worst_track_name      VARCHAR(200)  NULL AFTER worst_track_id,
    ADD COLUMN underrated_track_id   VARCHAR(64)   NULL AFTER worst_track_name,
    ADD COLUMN underrated_track_name VARCHAR(200)  NULL AFTER underrated_track_id,
    ADD COLUMN best_moment           VARCHAR(100)  NULL AFTER underrated_track_name,
    ADD COLUMN body                  VARCHAR(2000) NULL AFTER best_moment;
