package com.musicreview.server.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 400
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
    INVALID_REVIEW_FIELDS(HttpStatus.BAD_REQUEST, "리뷰 대상 타입에 맞지 않는 항목이 있습니다."),
    INVALID_ITEM_TYPE(HttpStatus.BAD_REQUEST, "type은 track, album, playlist 중 하나여야 합니다."),
    PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "현재 비밀번호가 일치하지 않습니다."),

    // 401
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."),
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 틀렸습니다."),

    // 403
    FORBIDDEN(HttpStatus.FORBIDDEN, "권한이 없습니다."),
    REVIEW_FORBIDDEN(HttpStatus.FORBIDDEN, "본인이 작성한 리뷰만 수정하거나 삭제할 수 있습니다."),

    // 404
    NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 경로를 찾을 수 없습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "리뷰를 찾을 수 없습니다."),
    SPOTIFY_NOT_FOUND(HttpStatus.NOT_FOUND, "Spotify에서 해당 항목을 찾을 수 없습니다."),

    // 405
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 HTTP 메서드입니다."),

    // 409
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    REVIEW_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 이 항목에 리뷰를 작성했습니다."),

    // 429
    SPOTIFY_RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "Spotify 요청이 많습니다. 잠시 후 다시 시도해 주세요."),

    // 5xx
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다."),
    SPOTIFY_UNAVAILABLE(HttpStatus.BAD_GATEWAY, "Spotify에 연결할 수 없습니다."),
    SPOTIFY_NOT_CONFIGURED(HttpStatus.SERVICE_UNAVAILABLE, "서버에 Spotify 연동 설정이 없습니다.");

    private final HttpStatus status;
    private final String message;
}
