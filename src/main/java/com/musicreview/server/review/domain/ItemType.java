package com.musicreview.server.review.domain;

import com.musicreview.server.global.exception.BusinessException;
import com.musicreview.server.global.exception.ErrorCode;

import java.util.Locale;

/** 리뷰 대상 타입. JSON은 대문자(TRACK), URL 경로는 소문자(track)를 씀 */
public enum ItemType {
    TRACK,
    ALBUM,
    PLAYLIST;

    /** 경로·쿼리 값(대소문자 무관) → ItemType. 잘못된 값이면 400 INVALID_ITEM_TYPE */
    public static ItemType from(String value) {
        if (value == null) {
            throw new BusinessException(ErrorCode.INVALID_ITEM_TYPE);
        }
        try {
            return ItemType.valueOf(value.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_ITEM_TYPE);
        }
    }
}
