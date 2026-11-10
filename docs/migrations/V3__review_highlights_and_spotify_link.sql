-- v2 → v3: 앨범·플리 리뷰 하이라이트, Spotify 계정 연동
-- 서버(mysql 프로필, ddl-auto=update)를 켜면 컬럼은 자동으로 추가됩니다.
-- 자동 추가가 안 됐거나 ddl-auto를 validate로 바꾼 뒤라면 직접 실행:
--   mysql -u root -p -e "source C:/Users/USER/Documents/back/docs/migrations/V3__review_highlights_and_spotify_link.sql"

USE music_review;

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
