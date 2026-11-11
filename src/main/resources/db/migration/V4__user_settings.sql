-- V4: 앱 화면 설정 (다크모드, 글자 크기)
-- 기존 회원은 기본값(기기 설정을 따름, 배율 1.00)이 채워진다.

ALTER TABLE users
    ADD COLUMN theme_mode VARCHAR(10)  NOT NULL DEFAULT 'SYSTEM' AFTER spotify_display_name,
    ADD COLUMN text_scale DECIMAL(3,2) NOT NULL DEFAULT 1.00     AFTER theme_mode;
